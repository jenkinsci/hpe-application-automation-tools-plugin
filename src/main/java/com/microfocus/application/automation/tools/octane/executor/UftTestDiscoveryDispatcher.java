/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.executor;

import com.google.inject.Inject;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.api.EntitiesService;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.exceptions.OctaneRestException;
import com.hp.octane.integrations.uft.UftTestDispatchUtils;
import com.hp.octane.integrations.uft.items.*;
import com.hp.octane.integrations.util.SdkStringUtils;
import com.microfocus.application.automation.tools.octane.ResultQueue;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationListener;
import com.microfocus.application.automation.tools.octane.configuration.ServerConfiguration;
import com.microfocus.application.automation.tools.octane.tests.AbstractSafeLoggingAsyncPeriodWork;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.TimeUnit2;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class is responsible to send discovered uft tests to Octane.
 * Class uses file-based queue so if octane or jenkins will be down before sending,
 * after connection is up - this dispatcher will send tests to Octane.
 * <p>
 * Actually list of discovered tests are persisted in job run directory. Queue contains only reference to that job run.
 */
@Extension
public class UftTestDiscoveryDispatcher extends AbstractSafeLoggingAsyncPeriodWork implements ConfigurationListener {

	private final static Logger logger = LogManager.getLogger(UftTestDiscoveryDispatcher.class);

	private final static int MAX_DISPATCH_TRIALS = 5;
	private static final String OCTANE_VERSION_SUPPORTING_TEST_RENAME = "12.60.3";
	private static String OCTANE_VERSION = null;

	private UftTestDiscoveryQueue queue;

	public UftTestDiscoveryDispatcher() {
		super("Uft Test Discovery Dispatcher");
	}


	@Override
	protected void doExecute(TaskListener listener) {
		if (queue.peekFirst() == null) {
			return;
		}

		logger.warn("Queue size  " + queue.size());

		if (!OctaneSDK.getInstance().getConfigurationService().isConfigurationValid()) {
			logger.warn("There are pending discovered UFT tests, but MQM server configuration is not valid, results can't be submitted");
			return;
		}

		EntitiesService entitiesService = OctaneSDK.getInstance().getEntitiesService();
		ResultQueue.QueueItem item = null;
		try {
			while ((item = queue.peekFirst()) != null) {

				Job project = (Job) Jenkins.getInstance().getItemByFullName(item.getProjectName());
				if (project == null) {
					logger.warn("Project [" + item.getProjectName() + "] no longer exists, pending discovered tests can't be submitted");
					queue.remove();
					continue;
				}

				Run build = project.getBuildByNumber(item.getBuildNumber());
				if (build == null) {
					logger.warn("Build [" + item.getProjectName() + "#" + item.getBuildNumber() + "] no longer exists, pending discovered tests can't be submitted");
					queue.remove();
					continue;
				}

				UftTestDiscoveryResult result = UFTTestDetectionService.readDetectionResults(build);
				if (result == null) {
					logger.warn("Build [" + item.getProjectName() + "#" + item.getBuildNumber() + "] no longer contains valid detection result file");
					queue.remove();
					continue;
				}

				logger.warn("Persistence [" + item.getProjectName() + "#" + item.getBuildNumber() + "]");
				dispatchDetectionResults(item, entitiesService, result);
				queue.remove();
			}
        } catch (OctaneRestException e) {
            String reasonDesc = StringUtils.isNotEmpty(e.getData().getDescriptionTranslated()) ? e.getData().getDescriptionTranslated() : e.getData().getDescription();
            if (e.getResponseStatus() == HttpStatus.SC_FORBIDDEN) {
                logger.error("Failed to  persist discovery of [" + item.getProjectName() + "#" + item.getBuildNumber() + "]  because of lacking Octane permission : " + reasonDesc);
            } else {
                logger.error("Failed to  persist discovery of [" + item.getProjectName() + "#" + item.getBuildNumber() + "]  : " + reasonDesc);
            }
            queue.remove();
        } catch (Exception e) {
            if (item != null) {
                item.incrementFailCount();
                if (item.incrementFailCount() > MAX_DISPATCH_TRIALS) {
                    queue.remove();
                    logger.error("Failed to  persist discovery of [" + item.getProjectName() + "#" + item.getBuildNumber() + "]  after " + MAX_DISPATCH_TRIALS + " trials");
                }
            }
        }
	}

	private static void dispatchDetectionResults(ResultQueue.QueueItem item, EntitiesService entitiesService, UftTestDiscoveryResult result) {
		//Check if there is diff in discovery and server status
		//for example : discovery found new test , but it already exist in server , instead of create new tests we will do update test
		if (result.isFullScan()) {
			UftTestDispatchUtils.prepareDispatchingForFullSync(entitiesService, result);

		} else {
			if (isOctaneSupportTestRename(entitiesService)) {
				handleMovedTests(result);
				handleMovedDataTables(result);
			}

			validateTestDiscoveryAndCompleteTestIdsForScmChangeDetection(entitiesService, result);
			validateTestDiscoveryAndCompleteDataTableIdsForScmChangeDetection(entitiesService, result);


			UftTestDispatchUtils.removeItemsWithStatusNone(result.getAllTests());
			UftTestDispatchUtils.removeItemsWithStatusNone(result.getAllScmResourceFiles());
		}

		//publish final results
		FreeStyleProject project = (FreeStyleProject) Jenkins.getInstance().getItemByFullName(item.getProjectName());
		FilePath subWorkspace = project.getWorkspace().child("_Final_Detection_Results");
		try {
			if (!subWorkspace.exists()) {
				subWorkspace.mkdirs();
			}
			File reportXmlFile = new File(subWorkspace.getRemote(), "final_detection_result_build_" + item.getBuildNumber() + ".xml");
			result.writeToFile(reportXmlFile);
		} catch (IOException | InterruptedException | JAXBException e) {
			logger.error("Failed to write final_detection_result file :" + e.getMessage());
		}

		//dispatch
		JobRunContext jobRunContext = JobRunContext.create(item.getProjectName(), item.getBuildNumber());
		UftTestDispatchUtils.dispatchDiscoveryResult(entitiesService, result, jobRunContext, null);
	}


	private static boolean validateTestDiscoveryAndCompleteDataTableIdsForScmChangeDetection(EntitiesService entitiesService, UftTestDiscoveryResult result) {
		boolean hasDiff = false;
		Set<String> allNames = new HashSet<>();
		for (ScmResourceFile file : result.getAllScmResourceFiles()) {
			if (file.getIsMoved()) {
				allNames.add(file.getOldName());
			} else {
				allNames.add(file.getName());
			}
		}

		//GET DataTables FROM OCTANE
		Map<String, Entity> octaneEntityMapByRelativePath = UftTestDispatchUtils.getDataTablesFromServer(entitiesService, Long.parseLong(result.getWorkspaceId()), Long.parseLong(result.getScmRepositoryId()), allNames);


		//MATCHING
		for (ScmResourceFile file : result.getAllScmResourceFiles()) {

			String key = file.getIsMoved() ? file.getOldRelativePath() : file.getRelativePath();
			Entity octaneFile = octaneEntityMapByRelativePath.get(key);

            boolean octaneFileFound = (octaneFile != null);
            if (octaneFileFound) {
                file.setId(octaneFile.getId());
            }

            switch (file.getOctaneStatus()) {
                case DELETED:
                    if (!octaneFileFound) {
                        //file that is marked to be deleted - doesn't exist in Octane - do nothing
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                case MODIFIED:
                    if (!octaneFileFound) {
                        //updated file that has no matching in Octane, possibly was remove from Octane. So we move it to new
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NEW);
                    }
                    break;
                case NEW:
                    if (octaneFileFound) {
                        //new file was found in Octane - do nothing(there is nothing to update)
                        hasDiff = true;
                        file.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                default:
                    //do nothing
            }
        }

        return hasDiff;
    }

    /**
     * This method try to find ids of updated and deleted tests for scm change detection
     * if test is found on server - update id of discovered test
     * if test is not found and test is marked for update - move it to new tests (possibly test was deleted on server)
     *
     * @return true if there were changes comparing to discoverede results
     */
    private static boolean validateTestDiscoveryAndCompleteTestIdsForScmChangeDetection(EntitiesService entitiesService, UftTestDiscoveryResult result) {
        boolean hasDiff = false;

        Set<String> allTestNames = new HashSet<>();
        for (AutomatedTest test : result.getAllTests()) {
            if (test.getIsMoved()) {
                allTestNames.add(test.getOldName());
            } else {
                allTestNames.add(test.getName());
            }
        }

        //GET TESTS FROM OCTANE
        Map<String, Entity> octaneTestsMapByKey = UftTestDispatchUtils.getTestsFromServer(entitiesService, Long.parseLong(result.getWorkspaceId()), Long.parseLong(result.getScmRepositoryId()), allTestNames);


        //MATCHING
        for (AutomatedTest discoveredTest : result.getAllTests()) {
            String key = discoveredTest.getIsMoved()
                    ? UftTestDispatchUtils.createKey(discoveredTest.getOldPackage(), discoveredTest.getOldName())
                    : UftTestDispatchUtils.createKey(discoveredTest.getPackage(), discoveredTest.getName());
            Entity octaneTest = octaneTestsMapByKey.get(key);
            boolean octaneTestFound = (octaneTest != null);
            if (octaneTestFound) {
                discoveredTest.setId(octaneTest.getId());
            }
            switch (discoveredTest.getOctaneStatus()) {
                case DELETED:
                    if (!octaneTestFound) {
                        //discoveredTest that is marked to be deleted - doesn't exist in Octane - do nothing
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.NONE);
                    }
                    break;
                case MODIFIED:
                    if (!octaneTestFound) {
                        //updated discoveredTest that has no matching in Octane, possibly was remove from Octane. So we move it to new tests
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.NEW);
                    } else {
                        boolean testsEqual = UftTestDispatchUtils.checkTestEquals(discoveredTest, octaneTest);
                        if (testsEqual) { //if equal - skip
                            discoveredTest.setOctaneStatus(OctaneStatus.NONE);
                        }
                    }
                    break;
                case NEW:
                    if (octaneTestFound) {
                        //new discoveredTest was found in Octane - move it to update
                        hasDiff = true;
                        discoveredTest.setOctaneStatus(OctaneStatus.MODIFIED);
                    }
                    break;
                default:
                    //do nothing
            }
        }

        return hasDiff;
    }

    @Override
    public long getRecurrencePeriod() {
        String value = System.getProperty("UftTestDiscoveryDispatcher.Period"); // let's us config the recurrence period. default is 60 seconds.
        if (!SdkStringUtils.isEmpty(value)) {
            return Long.valueOf(value);
        }
        return TimeUnit2.SECONDS.toMillis(30);
    }

    @Inject
    public void setTestResultQueue(UftTestDiscoveryQueue queue) {
        this.queue = queue;
    }

    /**
     * Queue that current run contains discovered tests
     *
     * @param projectName jobs name
     * @param buildNumber build number
     */
    public void enqueueResult(String projectName, int buildNumber) {
        queue.add(projectName, buildNumber);
    }

    private static void handleMovedTests(UftTestDiscoveryResult result) {
        List<AutomatedTest> newTests = result.getNewTests();
        List<AutomatedTest> deletedTests = result.getDeletedTests();
        if (!newTests.isEmpty() && !deletedTests.isEmpty()) {
            Map<String, AutomatedTest> dst2Test = new HashMap<>();
            Map<AutomatedTest, AutomatedTest> deleted2newMovedTests = new HashMap<>();
            for (AutomatedTest newTest : newTests) {
                if (SdkStringUtils.isNotEmpty(newTest.getChangeSetDst())) {
                    dst2Test.put(newTest.getChangeSetDst(), newTest);
                }
            }
            for (AutomatedTest deletedTest : deletedTests) {
                if (SdkStringUtils.isNotEmpty(deletedTest.getChangeSetDst()) && dst2Test.containsKey(deletedTest.getChangeSetDst())) {
                    AutomatedTest newTest = dst2Test.get(deletedTest.getChangeSetDst());
                    deleted2newMovedTests.put(deletedTest, newTest);
                }
            }

            for (Map.Entry<AutomatedTest, AutomatedTest> entry : deleted2newMovedTests.entrySet()) {
                AutomatedTest deletedTest = entry.getKey();
                AutomatedTest newTest = entry.getValue();

                newTest.setIsMoved(true);
                newTest.setOldName(deletedTest.getName());
                newTest.setOldPackage(deletedTest.getPackage());
                newTest.setOctaneStatus(OctaneStatus.MODIFIED);

                result.getAllTests().remove(deletedTest);
            }
        }
    }

    private static void handleMovedDataTables(UftTestDiscoveryResult result) {
        List<ScmResourceFile> newItems = result.getNewScmResourceFiles();
        List<ScmResourceFile> deletedItems = result.getDeletedScmResourceFiles();
        if (!newItems.isEmpty() && !deletedItems.isEmpty()) {
            Map<String, ScmResourceFile> dst2File = new HashMap<>();
            Map<ScmResourceFile, ScmResourceFile> deleted2newMovedFiles = new HashMap<>();
            for (ScmResourceFile newFile : newItems) {
                if (SdkStringUtils.isNotEmpty(newFile.getChangeSetDst())) {
                    dst2File.put(newFile.getChangeSetDst(), newFile);
                }
            }
            for (ScmResourceFile deletedFile : deletedItems) {
                if (SdkStringUtils.isNotEmpty(deletedFile.getChangeSetDst()) && dst2File.containsKey(deletedFile.getChangeSetDst())) {
                    ScmResourceFile newFile = dst2File.get(deletedFile.getChangeSetDst());
                    deleted2newMovedFiles.put(deletedFile, newFile);
                }
            }

            for (Map.Entry<ScmResourceFile, ScmResourceFile> entry : deleted2newMovedFiles.entrySet()) {
                ScmResourceFile deletedFile = entry.getKey();
                ScmResourceFile newFile = entry.getValue();

                newFile.setIsMoved(true);
                newFile.setOldName(deletedFile.getName());
                newFile.setOldRelativePath(deletedFile.getRelativePath());
                newFile.setOctaneStatus(OctaneStatus.MODIFIED);

                result.getAllScmResourceFiles().remove(deletedFile);
            }
        }
    }

    private static boolean isOctaneSupportTestRename(EntitiesService entitiesService) {
        try {
            String octane_version = getOctaneVersion(entitiesService);
            boolean supportTestRename = (octane_version != null && versionCompare(OCTANE_VERSION_SUPPORTING_TEST_RENAME, octane_version) <= 0);
            logger.warn("Support test rename = " + supportTestRename);
            return supportTestRename;
        } catch (Exception e) {//can occur if user doesnot have permission to get octane version
            logger.warn("Failed to check isOctaneSupportTestRename : " + e.getMessage());
            return false;
        }
    }

    private static String getOctaneVersion(EntitiesService entitiesService) {

        if (OCTANE_VERSION == null) {
            List<Entity> entities = entitiesService.getEntities(null, "server_version", null, null);
            if (entities.size() == 1) {
                Entity entity = entities.get(0);
                OCTANE_VERSION = entity.getStringValue("version");
                logger.warn("Received Octane version - " + OCTANE_VERSION);

            } else {
                logger.error(String.format("Request for Octane version returned %s items. return version is not defined.", entities.size()));
            }
        }

        return OCTANE_VERSION;
    }

    @Override
    public void onChanged(ServerConfiguration conf, ServerConfiguration oldConf) {
        OCTANE_VERSION = null;
    }

    /**
     * Compares two version strings.
     * <p>
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     * The result is a positive integer if str1 is _numerically_ greater than str2.
     * The result is zero if the strings are _numerically_ equal.
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     */
    private static Integer versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(vals1.length - vals2.length);
        }
    }

}
