/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.executor.TestSuiteExecutionInfo;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.integrations.dto.pipelines.BuildHistory;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.scm.SCMData;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.exceptions.ConfigurationException;
import com.hp.octane.integrations.exceptions.PermissionException;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import com.hpe.application.automation.tools.model.OctaneServerSettingsModel;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.octane.configuration.ServerConfiguration;
import com.hpe.application.automation.tools.octane.executor.ExecutorConnectivityService;
import com.hpe.application.automation.tools.octane.executor.TestExecutionJobCreatorService;
import com.hpe.application.automation.tools.octane.executor.UftJobCleaner;
import com.hpe.application.automation.tools.octane.model.ModelFactory;
import com.hpe.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.hpe.application.automation.tools.octane.model.processors.projects.AbstractProjectProcessor;
import com.hpe.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.hpe.application.automation.tools.octane.model.processors.scm.SCMProcessor;
import com.hpe.application.automation.tools.octane.model.processors.scm.SCMProcessors;
import hudson.ProxyConfiguration;
import hudson.model.*;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.acegisecurity.AccessDeniedException;
import org.acegisecurity.context.SecurityContext;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Base implementation of SPI(service provider interface) of Octane CI SDK for Jenkins
 */
public class CIJenkinsServicesImpl extends CIPluginServicesBase {
    private static final Logger logger = LogManager.getLogger(CIJenkinsServicesImpl.class);
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    @Override
    public CIServerInfo getServerInfo() {
        CIServerInfo result = dtoFactory.newDTO(CIServerInfo.class);
        String serverUrl = Jenkins.getInstance().getRootUrl();
        if (serverUrl != null && serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        OctaneServerSettingsModel model = ConfigurationService.getModel();
        result.setType(CIServerTypes.JENKINS)
                .setVersion(Jenkins.VERSION)
                .setUrl(serverUrl)
                .setInstanceId(model.getIdentity())
                .setInstanceIdFrom(model.getIdentityFrom())
                .setSendingTime(System.currentTimeMillis())
                .setImpersonatedUser(model.getImpersonatedUser());
        return result;
    }

    @Override
    public CIPluginInfo getPluginInfo() {
        CIPluginInfo result = dtoFactory.newDTO(CIPluginInfo.class);
        result.setVersion(ConfigurationService.getPluginVersion());
        return result;
    }

    @Override
    public File getAllowedOctaneStorage() {
        return new File(Jenkins.getInstance().getRootDir(), "userContent");
    }

    @Override
    public File getPredictiveOctanePath() {
        return new File(Jenkins.getInstance().getRootDir(), "predictive");
    }

    @Override
    public OctaneConfiguration getOctaneConfiguration() {
        OctaneConfiguration result = null;
        ServerConfiguration serverConfiguration = ConfigurationService.getServerConfiguration();
        if (serverConfiguration.location != null && !serverConfiguration.location.isEmpty() &&
                serverConfiguration.sharedSpace != null && !serverConfiguration.sharedSpace.isEmpty()) {
            result = dtoFactory.newDTO(OctaneConfiguration.class)
                    .setUrl(serverConfiguration.location)
                    .setSharedSpace(serverConfiguration.sharedSpace)
                    .setApiKey(serverConfiguration.username)
                    .setSecret(serverConfiguration.password.getPlainText());
        }
        return result;
    }

    @Override
    public CIProxyConfiguration getProxyConfiguration(String targetHost) {
        CIProxyConfiguration result = null;
        ProxyConfiguration proxy = Jenkins.getInstance().proxy;
        if (proxy != null) {
            boolean noProxyHost = false;
            for (Pattern pattern : proxy.getNoProxyHostPatterns()) {
                if (pattern.matcher(targetHost).find()) {
                    noProxyHost = true;
                    break;
                }
            }
            if (!noProxyHost) {
                result = dtoFactory.newDTO(CIProxyConfiguration.class)
                        .setHost(proxy.name)
                        .setPort(proxy.port)
                        .setUsername(proxy.getUserName())
                        .setPassword(proxy.getPassword());
            }
        }
        return result;
    }

    @Override
    public CIJobsList getJobsList(boolean includeParameters) {
        SecurityContext securityContext = startImpersonation();
        CIJobsList result = dtoFactory.newDTO(CIJobsList.class);
        PipelineNode tmpConfig;
        TopLevelItem tmpItem;
        List<PipelineNode> list = new ArrayList<>();
        try {
            boolean hasReadPermission = Jenkins.getInstance().hasPermission(Item.READ);
            if (!hasReadPermission) {
                stopImpersonation(securityContext);
                throw new PermissionException(403);
            }
            List<String> itemNames = (List<String>) Jenkins.getInstance().getTopLevelItemNames();
            for (String name : itemNames) {
                tmpItem = Jenkins.getInstance().getItem(name);

                try {
                    if (tmpItem instanceof AbstractProject) {
                        AbstractProject abstractProject = (AbstractProject) tmpItem;
                        if (abstractProject.isDisabled()) {
                            continue;
                        }
                        tmpConfig = dtoFactory.newDTO(PipelineNode.class)
                                .setJobCiId(JobProcessorFactory.getFlowProcessor(abstractProject).getTranslateJobName())
                                .setName(name);
                        if (includeParameters) {
                            tmpConfig.setParameters(ParameterProcessors.getConfigs(abstractProject));
                        }
                        list.add(tmpConfig);
                    } else if (tmpItem.getClass().getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob")) {
                        Job tmpJob = (Job) tmpItem;
                        tmpConfig = dtoFactory.newDTO(PipelineNode.class)
                                .setJobCiId(JobProcessorFactory.getFlowProcessor(tmpJob).getTranslateJobName())
                                .setName(name);
                        if (includeParameters) {
                            tmpConfig.setParameters(ParameterProcessors.getConfigs(tmpJob));
                        }
                        list.add(tmpConfig);
                    } else if (tmpItem.getClass().getName().equals("com.cloudbees.hudson.plugins.folder.Folder")) {
                        for (Job tmpJob : tmpItem.getAllJobs()) {
                            tmpConfig = dtoFactory.newDTO(PipelineNode.class)
                                    .setJobCiId(JobProcessorFactory.getFlowProcessor(tmpJob).getTranslateJobName())
                                    .setName(tmpJob.getName());
                            if (includeParameters) {
                                tmpConfig.setParameters(ParameterProcessors.getConfigs(tmpJob));
                            }
                            list.add(tmpConfig);
                        }
                    } else {
                        logger.info("item '" + name + "' is not of supported type");
                    }
                } catch (Throwable e) {
                    logger.error("Failed to add job '" + name + "' to JobList  : " + e.getClass().getCanonicalName() + " - " + e.getMessage(), e);
                    //throw e;
                }

            }
            result.setJobs(list.toArray(new PipelineNode[list.size()]));
            stopImpersonation(securityContext);
        } catch (AccessDeniedException e) {
            stopImpersonation(securityContext);
            throw new PermissionException(403);
        }
        return result;
    }


    @Override
    public PipelineNode getPipeline(String rootJobCiId) {
        PipelineNode result;
        SecurityContext securityContext = startImpersonation();
        boolean hasRead = Jenkins.getInstance().hasPermission(Item.READ);
        if (!hasRead) {
            stopImpersonation(securityContext);
            throw new PermissionException(403);
        }
        Job project = getJobByRefId(rootJobCiId);
        if (project != null) {
            result = ModelFactory.createStructureItem(project);
            stopImpersonation(securityContext);
            return result;
        } else {
            //todo: check error message(s)
            logger.warn("Failed to get project from jobRefId: '" + rootJobCiId + "' check plugin user Job Read/Overall Read permissions / project name");
            stopImpersonation(securityContext);
            throw new ConfigurationException(404);
        }
    }

    private SecurityContext startImpersonation() {
        String user = ConfigurationService.getModel().getImpersonatedUser();
        SecurityContext originalContext = null;
        if (user != null && !user.equalsIgnoreCase("")) {
            User jenkinsUser = User.get(user, false);
            if (jenkinsUser != null) {
                originalContext = ACL.impersonate(jenkinsUser.impersonate());
            } else {
                throw new PermissionException(401);
            }
        } else {
            logger.info("No user set to impersonating to. Operations will be done using Anonymous user");
        }
        return originalContext;
    }

    private void stopImpersonation(SecurityContext originalContext) {
        if (originalContext != null) {
            ACL.impersonate(originalContext.getAuthentication());
        } else {
            logger.warn("Could not roll back impersonation, originalContext is null ");
        }
    }

    @Override
    public void runPipeline(String jobCiId, String originalBody) {
        SecurityContext securityContext = startImpersonation();
        Job job = getJobByRefId(jobCiId);
        if (job != null) {
            boolean hasBuildPermission = job.hasPermission(Item.BUILD);
            if (!hasBuildPermission) {
                stopImpersonation(securityContext);
                throw new PermissionException(403);
            }
            if (job instanceof AbstractProject || job.getClass().getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob")) {
                doRunImpl(job, originalBody);
            }
            stopImpersonation(securityContext);
        } else {
            stopImpersonation(securityContext);
            throw new ConfigurationException(404);
        }
    }

    @Override
    public SnapshotNode getSnapshotLatest(String jobCiId, boolean subTree) {
        SecurityContext securityContext = startImpersonation();
        SnapshotNode result = null;
        Job job = getJobByRefId(jobCiId);
        if (job != null) {
            Run run = job.getLastBuild();
            if (run != null) {
                result = ModelFactory.createSnapshotItem(run, subTree);
            }
        }
        stopImpersonation(securityContext);
        return result;
    }

    @Override
    public SnapshotNode getSnapshotByNumber(String jobCiId, String buildCiId, boolean subTree) {
        SecurityContext securityContext = startImpersonation();

        SnapshotNode result = null;
        Job job = getJobByRefId(jobCiId);

        Integer buildNumber = null;
        try {
            buildNumber = Integer.parseInt(buildCiId);
        } catch (NumberFormatException nfe) {
            logger.error("failed to parse build CI ID to build number, " + nfe.getMessage(), nfe);
        }
        if (job != null && buildNumber != null) {
            Run build = job.getBuildByNumber(buildNumber);
            if (build != null) {
                result = ModelFactory.createSnapshotItem(build, subTree);
            }
        }

        stopImpersonation(securityContext);
        return result;
    }

    @Override
    public BuildHistory getHistoryPipeline(String jobCiId, String originalBody) {
        SecurityContext securityContext = startImpersonation();
        BuildHistory buildHistory = dtoFactory.newDTO(BuildHistory.class);
        Job job = getJobByRefId(jobCiId);
        AbstractProject project;
        if (job instanceof AbstractProject) {
            project = (AbstractProject) job;
            SCMData scmData;
            Set<User> users;
            SCMProcessor scmProcessor = SCMProcessors.getAppropriate(project.getScm().getClass().getName());

            int numberOfBuilds = 5;

            //TODO : check if it works!!
            if (originalBody != null && !originalBody.isEmpty()) {
                JSONObject bodyJSON = JSONObject.fromObject(originalBody);
                if (bodyJSON.has("numberOfBuilds")) {
                    numberOfBuilds = bodyJSON.getInt("numberOfBuilds");
                }
            }
            List<Run> result = project.getLastBuildsOverThreshold(numberOfBuilds, Result.ABORTED); // get last five build with result that better or equal failure
            for (int i = 0; i < result.size(); i++) {
                AbstractBuild build = (AbstractBuild) result.get(i);
                scmData = null;
                users = null;
                if (build != null) {
                    if (scmProcessor != null) {
                        scmData = scmProcessor.getSCMData(build);
                        users = build.getCulprits();
                    }
                    buildHistory.addBuild(build.getResult().toString(), String.valueOf(build.getNumber()), build.getTimestampString(), String.valueOf(build.getStartTimeInMillis()), String.valueOf(build.getDuration()), scmData, ModelFactory.createScmUsersList(users));
                }
            }
            AbstractBuild lastSuccessfulBuild = null;
            AbstractBuild lastProjectBuild = project.getLastBuild();
            if (lastProjectBuild != null) {
                lastSuccessfulBuild = (AbstractBuild) lastProjectBuild.getPreviousSuccessfulBuild();
            }
            if (lastSuccessfulBuild != null) {
                scmData = null;
                users = null;
                if (scmProcessor != null) {
                    scmData = scmProcessor.getSCMData(lastSuccessfulBuild);
                    users = lastSuccessfulBuild.getCulprits();
                }
                buildHistory.addLastSuccesfullBuild(lastSuccessfulBuild.getResult().toString(), String.valueOf(lastSuccessfulBuild.getNumber()), lastSuccessfulBuild.getTimestampString(), String.valueOf(lastSuccessfulBuild.getStartTimeInMillis()), String.valueOf(lastSuccessfulBuild.getDuration()), scmData, ModelFactory.createScmUsersList(users));
            }
            AbstractBuild lastBuild = project.getLastBuild();
            if (lastBuild != null) {
                scmData = null;
                users = null;
                if (scmProcessor != null) {
                    scmData = scmProcessor.getSCMData(lastBuild);
                    users = lastBuild.getCulprits();
                }

                if (lastBuild.getResult() == null) {
                    buildHistory.addLastBuild("building", String.valueOf(lastBuild.getNumber()), lastBuild.getTimestampString(), String.valueOf(lastBuild.getStartTimeInMillis()), String.valueOf(lastBuild.getDuration()), scmData, ModelFactory.createScmUsersList(users));
                } else {
                    buildHistory.addLastBuild(lastBuild.getResult().toString(), String.valueOf(lastBuild.getNumber()), lastBuild.getTimestampString(), String.valueOf(lastBuild.getStartTimeInMillis()), String.valueOf(lastBuild.getDuration()), scmData, ModelFactory.createScmUsersList(users));
                }
            }
            stopImpersonation(securityContext);
        } else {
            logger.warn("non supported flow");
        }
        return buildHistory;
    }

    //  TODO: implement
    @Override
    public TestsResult getTestsResult(String jobId, String buildNumber) {
        return null;
    }

    //  TODO: the below flow should go via JobProcessor, once scheduleBuild will be implemented for all of them
    private void doRunImpl(Job job, String originalBody) {
        if (job instanceof AbstractProject) {
            AbstractProject project = (AbstractProject) job;
            int delay = project.getQuietPeriod();
            ParametersAction parametersAction = new ParametersAction();

            if (originalBody != null && !originalBody.isEmpty()) {
                JSONObject bodyJSON = JSONObject.fromObject(originalBody);

                //  delay
                if (bodyJSON.has("delay") && bodyJSON.get("delay") != null) {
                    delay = bodyJSON.getInt("delay");
                }

                //  parameters
                if (bodyJSON.has("parameters") && bodyJSON.get("parameters") != null) {
                    JSONArray paramsJSON = bodyJSON.getJSONArray("parameters");
                    parametersAction = new ParametersAction(createParameters(project, paramsJSON));
                }
            }

            project.scheduleBuild(delay, new Cause.RemoteCause(getOctaneConfiguration() == null ? "non available URL" : getOctaneConfiguration().getUrl(), "octane driven execution"), parametersAction);
        } else if (job.getClass().getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob")) {
            AbstractProjectProcessor workFlowJobProcessor = JobProcessorFactory.getFlowProcessor(job);
            workFlowJobProcessor.scheduleBuild(originalBody);
        }
    }

    private List<ParameterValue> createParameters(AbstractProject project, JSONArray paramsJSON) {
        List<ParameterValue> result = new ArrayList<>();
        boolean parameterHandled;
        ParameterValue tmpValue;
        ParametersDefinitionProperty paramsDefProperty = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
        if (paramsDefProperty != null) {
            for (ParameterDefinition paramDef : paramsDefProperty.getParameterDefinitions()) {
                parameterHandled = false;
                for (int i = 0; i < paramsJSON.size(); i++) {
                    JSONObject paramJSON = paramsJSON.getJSONObject(i);
                    if (paramJSON.has("name") && paramJSON.get("name") != null && paramJSON.get("name").equals(paramDef.getName())) {
                        tmpValue = null;
                        switch (CIParameterType.fromValue(paramJSON.getString("type"))) {
                            case FILE:
                                try {
                                    FileItemFactory fif = new DiskFileItemFactory();
                                    FileItem fi = fif.createItem(paramJSON.getString("name"), "text/plain", false, paramJSON.getString("file"));
                                    fi.getOutputStream().write(DatatypeConverter.parseBase64Binary(paramJSON.getString("value")));
                                    tmpValue = new FileParameterValue(paramJSON.getString("name"), fi);
                                } catch (IOException ioe) {
                                    logger.warn("failed to process file parameter", ioe);
                                }
                                break;
                            case NUMBER:
                                tmpValue = new StringParameterValue(paramJSON.getString("name"), paramJSON.get("value").toString());
                                break;
                            case STRING:
                                tmpValue = new StringParameterValue(paramJSON.getString("name"), paramJSON.getString("value"));
                                break;
                            case BOOLEAN:
                                tmpValue = new BooleanParameterValue(paramJSON.getString("name"), paramJSON.getBoolean("value"));
                                break;
                            case PASSWORD:
                                tmpValue = new PasswordParameterValue(paramJSON.getString("name"), paramJSON.getString("value"));
                                break;
                            default:
                                break;
                        }
                        if (tmpValue != null) {
                            result.add(tmpValue);
                            parameterHandled = true;
                        }
                        break;
                    }
                }
                if (!parameterHandled) {
                    if (paramDef instanceof FileParameterDefinition) {
                        FileItemFactory fif = new DiskFileItemFactory();
                        FileItem fi = fif.createItem(paramDef.getName(), "text/plain", false, "");
                        try {
                            fi.getOutputStream().write(new byte[0]);
                        } catch (IOException ioe) {
                            logger.error("failed to create default value for file parameter '" + paramDef.getName() + "'", ioe);
                        }
                        tmpValue = new FileParameterValue(paramDef.getName(), fi);
                        result.add(tmpValue);
                    } else {
                        result.add(paramDef.getDefaultParameterValue());
                    }
                }
            }
        }
        return result;
    }

    private Job getJobByRefId(String jobRefId) {
        Job result = null;
        if (jobRefId != null) {
            try {
                jobRefId = URLDecoder.decode(jobRefId, "UTF-8");
                TopLevelItem item = getTopLevelItem(jobRefId);
                if (item != null && item instanceof Job) {
                    result = (Job) item;
                } else if (jobRefId.contains("/") && item == null) {
                    String newJobRefId = jobRefId.substring(0, jobRefId.indexOf("/"));
                    item = getTopLevelItem(newJobRefId);
                    if (item != null) {
                        Collection<? extends Job> allJobs = item.getAllJobs();
                        for (Job job : allJobs) {
                            if (jobRefId.endsWith(job.getName())) {
                                result = job;
                                break;
                            }
                        }
                    }
                }
            } catch (UnsupportedEncodingException uee) {
                logger.error("failed to decode job ref ID '" + jobRefId + "'", uee);
            }
        }
        return result;
    }

    private TopLevelItem getTopLevelItem(String jobRefId) {
        TopLevelItem item;
        try {
            item = Jenkins.getInstance().getItem(jobRefId);
        } catch (AccessDeniedException e) {
            String user = ConfigurationService.getModel().getImpersonatedUser();
            if (user != null && !user.isEmpty()) {
                throw new PermissionException(403);
            } else {
                throw new PermissionException(405);
            }
        }
        return item;
    }

    @Override
    public void runTestDiscovery(DiscoveryInfo discoveryInfo) {
        SecurityContext securityContext = startImpersonation();
        try {
            TestExecutionJobCreatorService.runTestDiscovery(discoveryInfo);
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public void runTestSuiteExecution(TestSuiteExecutionInfo suiteExecutionInfo) {
        SecurityContext securityContext = startImpersonation();
        try {
            TestExecutionJobCreatorService.runTestSuiteExecution(suiteExecutionInfo);
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public OctaneResponse checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo) {
        SecurityContext securityContext = startImpersonation();
        try {
            return ExecutorConnectivityService.checkRepositoryConnectivity(testConnectivityInfo);
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public void deleteExecutor(String id) {
        SecurityContext securityContext = startImpersonation();
        try {
            UftJobCleaner.deleteExecutor(id);
        } finally {
            stopImpersonation(securityContext);
        }

    }

    @Override
    public OctaneResponse upsertCredentials(CredentialsInfo credentialsInfo) {
        SecurityContext securityContext = startImpersonation();
        try {
            return ExecutorConnectivityService.upsertRepositoryCredentials(credentialsInfo);
        } finally {
            stopImpersonation(securityContext);
        }
    }

}
