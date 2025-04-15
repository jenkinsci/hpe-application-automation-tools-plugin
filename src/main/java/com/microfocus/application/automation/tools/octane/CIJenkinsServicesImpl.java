/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.octane;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.hp.octane.integrations.CIPluginServices;
import com.hp.octane.integrations.OctaneClient;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.integrations.dto.entities.EntityConstants;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.MultiBranchType;
import com.hp.octane.integrations.dto.executor.CredentialsInfo;
import com.hp.octane.integrations.dto.executor.DiscoveryInfo;
import com.hp.octane.integrations.dto.executor.TestConnectivityInfo;
import com.hp.octane.integrations.dto.general.*;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameters;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.scm.Branch;
import com.hp.octane.integrations.dto.securityscans.FodServerConfiguration;
import com.hp.octane.integrations.dto.securityscans.SSCProjectConfiguration;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.integrations.dto.snapshots.CIBuildStatus;
import com.hp.octane.integrations.exceptions.ConfigurationException;
import com.hp.octane.integrations.exceptions.PermissionException;
import com.hp.octane.integrations.services.configurationparameters.FortifySSCTokenParameter;
import com.hp.octane.integrations.services.configurationparameters.UftTestRunnerFolderParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.configuration.*;
import com.microfocus.application.automation.tools.octane.executor.ExecutorConnectivityService;
import com.microfocus.application.automation.tools.octane.executor.TestExecutionJobCreatorService;
import com.microfocus.application.automation.tools.octane.executor.UftJobRecognizer;
import com.microfocus.application.automation.tools.octane.model.ModelFactory;
import com.microfocus.application.automation.tools.octane.model.processors.parameters.ParameterProcessors;
import com.microfocus.application.automation.tools.octane.model.processors.projects.AbstractProjectProcessor;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.model.processors.scm.SCMUtils;
import com.microfocus.application.automation.tools.octane.testrunner.TestsToRunConverterBuilder;
import com.microfocus.application.automation.tools.octane.tests.TestListener;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.microfocus.application.automation.tools.octane.tests.junit.JUnitExtension;
import com.microfocus.application.automation.tools.settings.RunnerMiscSettingsGlobalConfiguration;
import hudson.ProxyConfiguration;
import hudson.console.PlainTextConsoleOutputStream;
import hudson.matrix.MatrixConfiguration;
import hudson.maven.MavenModule;
import hudson.model.*;
import hudson.security.ACLContext;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;
import org.acegisecurity.AccessDeniedException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base implementation of SPI(service provider interface) of Octane CI SDK for Jenkins
 */

public class CIJenkinsServicesImpl extends CIPluginServices {

    //we going to print octaneAllowedStorage to system log, this flag help to avoid multiple prints
    private static boolean skipOctaneAllowedStoragePrint = false;
    private static Object skipOctaneAllowedStoragePrintLock = new Object();//this must be before SDKBasedLoggerProvider.getLogger

    private static final DTOFactory dtoFactory = DTOFactory.getInstance();
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(CIJenkinsServicesImpl.class);
    private static final java.util.logging.Logger systemLogger = java.util.logging.Logger.getLogger(CIJenkinsServicesImpl.class.getName());

    private static final String DEFAULT_BRANCHES_SEPARATOR = " ";

    @Override
    public CIServerInfo getServerInfo() {
        return getJenkinsServerInfo();
    }

    @Override
    public CIPluginInfo getPluginInfo() {
        CIPluginInfo result = dtoFactory.newDTO(CIPluginInfo.class);
        result.setVersion(ConfigurationService.getPluginVersion());
        return result;
    }

    @Override
    public void suspendCIEvents(boolean suspend) {
        OctaneServerSettingsModel model = ConfigurationService.getSettings(getInstanceId());
        model.setSuspend(suspend);
        ConfigurationService.configurePlugin(model);
        logger.info("suspend ci event: " + suspend);
    }

    @Override
    public File getAllowedOctaneStorage() {
        return getAllowedStorageFile();
    }

    @Override
    public CIProxyConfiguration getProxyConfiguration(URL targetUrl) {
        return getProxySupplier(targetUrl);
    }

    public static CIProxyConfiguration getProxySupplier(URL targetUrl) {
        CIProxyConfiguration result = null;
        ProxyConfiguration proxy = Jenkins.get().proxy;
        if (proxy != null) {
            boolean noProxyHost = false;
            for (Pattern pattern : proxy.getNoProxyHostPatterns()) {
                if (pattern.matcher(targetUrl.getHost()).matches()) {
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
    public CIJobsList getJobsList(boolean includeParameters, Long workspaceId) {
        ACLContext securityContext = startImpersonation(workspaceId);
        CIJobsList result = dtoFactory.newDTO(CIJobsList.class);
        Map<String, PipelineNode> jobsMap = new HashMap<>();

        try {
            long start = System.currentTimeMillis();
            Collection<String> jobNames = Jenkins.get().getJobNames();
            logger.debug("Get job names took {} ms", System.currentTimeMillis() - start );
            for (String jobName : jobNames) {
                String tempJobName = jobName;
                try {
                    long startProcessJob = System.currentTimeMillis();
                    Job tmpJob = (Job) Jenkins.get().getItemByFullName(tempJobName);

                    if (!isJobIsRelevantForPipelineModule(tmpJob)) {
                        continue;
                    }

                    PipelineNode tmpConfig;
                    if (tmpJob != null && JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME.equals(tmpJob.getParent().getClass().getName())) {
                        tempJobName = tmpJob.getParent().getFullName();
                        WorkflowMultiBranchProject parentItem = (WorkflowMultiBranchProject) Jenkins.get().getItem(tempJobName);
                        if( (parentItem != null && parentItem.isDisabled()) || jobsMap.containsKey(tempJobName)){
                            continue; //skip redundant creation config for multibranch job
                        }
                        tmpConfig = createPipelineNodeFromJobName(tempJobName);
                    } else {
                        tmpConfig = createPipelineNode(tempJobName, tmpJob, includeParameters);
                    }
                    jobsMap.put(tempJobName, tmpConfig);
                    if(System.currentTimeMillis() - startProcessJob > 2000){
                        logger.warn("Job {} took {} ms",tempJobName,System.currentTimeMillis() - startProcessJob);
                    }
                } catch (Throwable e) {
                    logger.error("failed to add job '" + tempJobName + "' to JobList", e);
                }
            }
            logger.info("Get job list took {} ms",System.currentTimeMillis() - start);

            if(jobsMap.isEmpty() && !Jenkins.get().hasPermission(Item.READ)){
                //it is possible that user doesn't have general READ permission
                // but has read permission to specific job, so we postponed this check to end
                String userName = ImpersonationUtil.getUserNameForImpersonation(getInstanceId(), workspaceId);
                if(StringUtils.isEmpty(userName)){
                    userName = "anonymous";
                }
                String msg = String.format("User %s does not have READ permission",userName);
                throw new PermissionException(msg, HttpStatus.SC_FORBIDDEN);
            }

            result.setJobs(jobsMap.values().toArray(new PipelineNode[0]));
        } catch (AccessDeniedException ade) {
            throw new PermissionException(HttpStatus.SC_FORBIDDEN);
        } finally {
            stopImpersonation(securityContext);
        }

        return result;
    }

    public static boolean isJobIsRelevantForPipelineModule(Job job){
        return !(job == null ||
                (job instanceof AbstractProject && ((AbstractProject) job).isDisabled()) ||
                job instanceof MatrixConfiguration ||
                job instanceof MavenModule);
    }

    @Override
    public PipelineNode getPipeline(String rootJobCiId) {
        ACLContext securityContext = startImpersonation();
        try {
            Item item = getItemByRefId(rootJobCiId);

            if (item == null) {
                logger.warn("Failed to get project from jobRefId: '" + rootJobCiId + "' check plugin user Job Read/" +
                        "Overall Read permissions / project name");
                throw new ConfigurationException(HttpStatus.SC_NOT_FOUND);
            }

            // Verify that the user has permission to access the job(Read permission for a specific job or global read)
            if (!Jenkins.get().hasPermission(Item.READ) && !item.hasPermission(Item.READ)) {
                logger.warn("Insufficient permissions to access jobRefId: '{}'.", rootJobCiId);
                throw new PermissionException(HttpStatus.SC_FORBIDDEN);
            }

            if (item instanceof Job) {
                return ModelFactory.createStructureItem((Job) item);
            }
            PipelineNode result = createPipelineNodeFromJobName(item.getFullName());
            if (item.getClass().getName().equals(JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME)) {
                WorkflowMultiBranchProject parentItem = (WorkflowMultiBranchProject) item;
                if (!parentItem.isDisabled()) {
                    addParametersAndDefaultBranchFromConfig(item, result);
                    result.setMultiBranchType(MultiBranchType.MULTI_BRANCH_PARENT);
                } else result = null;
            }
            return result;
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public void runPipeline(String jobCiId, CIParameters ciParameters) {
        ACLContext securityContext = startImpersonation();
        try {
            Job job = getJobByRefId(jobCiId);
            if (job != null) {
                if (job instanceof AbstractProject && ((AbstractProject) job).isDisabled()) {
                    //disabled job is not runnable and in this context we will handle it as 404
                    throw new ConfigurationException(HttpStatus.SC_NOT_FOUND);
                }
                boolean hasBuildPermission = job.hasPermission(Item.BUILD);
                if (!hasBuildPermission) {
                    stopImpersonation(securityContext);
                    throw new PermissionException(HttpStatus.SC_FORBIDDEN);
                }
                if (job instanceof AbstractProject || job.getClass().getName().equals(JobProcessorFactory.WORKFLOW_JOB_NAME)) {
                    doRunImpl(job, ciParameters);
                }
            } else {
                throw new ConfigurationException(HttpStatus.SC_NOT_FOUND);
            }
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public void stopPipelineRun(String jobCiId, CIParameters ciParameters) {
        ACLContext securityContext = startImpersonation();
        try {
            Job job = getJobByRefId(jobCiId);
            if (job != null) {
                boolean hasAbortPermissions = job.hasPermission(Item.CANCEL);
                if (!hasAbortPermissions) {
                    stopImpersonation(securityContext);
                    throw new PermissionException(HttpStatus.SC_FORBIDDEN);
                }
                if (job instanceof AbstractProject || job.getClass().getName().equals(JobProcessorFactory.WORKFLOW_JOB_NAME)) {
                    doStopImpl(job, ciParameters);
                }
            } else {
                throw new ConfigurationException(HttpStatus.SC_NOT_FOUND);
            }
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public CIBranchesList getBranchesList(String jobCiId, String filterBranchName) {
        ACLContext securityContext = startImpersonation();
        List<Branch> result = new ArrayList<>();
        try {
            Item item = Jenkins.get().getItemByFullName(jobCiId);
            if (item != null) {
                boolean hasRead = Jenkins.get().hasPermission(Item.READ);
                if (!hasRead) {
                    throw new PermissionException("Missing READ permission to job " + jobCiId,  HttpStatus.SC_FORBIDDEN);
                }
                if (item.getClass().getName().equals(JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME)) {
                    result = doGetListOfBranchesImpl(item, filterBranchName);
                }
                return dtoFactory.newDTO(CIBranchesList.class)
                        .setBranches(result);
            } else {
                throw new ConfigurationException(HttpStatus.SC_NOT_FOUND);
            }
        } finally {
            stopImpersonation(securityContext);
        }
    }

    private List<Branch> doGetListOfBranchesImpl(Item item, String filterBranchName) {
        Collection<? extends Job> allJobs = item.getAllJobs();

        return allJobs.stream().filter(job -> getDisplayNameFromJob(job).equals(filterBranchName))
                .map(job -> dtoFactory.newDTO(Branch.class)
                        .setName(job.getDisplayName())
                        .setInternalId(job.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public CIBuildStatusInfo getJobBuildStatus(String jobCiId, String parameterName, String parameterValue) {
        ACLContext securityContext = startImpersonation();
        try {
            Job job = getJobByRefId(jobCiId);
            boolean hasRead = Jenkins.get().hasPermission(Item.READ);
            if (!hasRead) {
                throw new PermissionException("Missing READ permission to job " + jobCiId,  HttpStatus.SC_FORBIDDEN);
            }
            if(job == null) {
                return getUnavailableJobStatus(jobCiId, parameterName, parameterValue);
            } else {
                AbstractProjectProcessor jobProcessor = JobProcessorFactory.getFlowProcessor(job);
                return jobProcessor.getBuildStatus(parameterName, parameterValue);
            }
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public InputStream getTestsResult(String jobId, String buildId) {
        ACLContext originalContext = startImpersonation();
        try {
            InputStream result = null;
            Run run = getRunByRefNames(jobId, buildId);
            if (run != null) {
                try {
                    result = new FileInputStream(run.getRootDir() + File.separator + TestListener.TEST_RESULT_FILE);
                } catch (Exception fnfe) {
                    logger.error("'" + TestListener.TEST_RESULT_FILE + "' file no longer exists, test results of '" + jobId + " #" + buildId + "' won't be pushed to Octane", fnfe);
                }
                tryRemoveTempTestResultFile(run);
            } else {
                logger.error("build '" + jobId + " #" + buildId + "' not found");
            }
            return result;
        } finally {
            stopImpersonation(originalContext);
        }
    }

    private void tryRemoveTempTestResultFile(Run run) {
        try {
            File[] matches = run.getRootDir().listFiles((dir, name) -> name.startsWith(JUnitExtension.TEMP_TEST_RESULTS_FILE_NAME_PREFIX));
            if (matches != null) {
                for (File f : matches) {
                    try {
                        Files.deleteIfExists(f.toPath());
                    } catch (Exception e) {
                        logger.error("Failed to delete the temp test result file at '" + f.getPath() + "'", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Fail to tryRemoveTempTestResultFile : " + e.getMessage());
        }
    }

    @Override
    public InputStream getBuildLog(String jobId, String buildId) {
        ACLContext originalContext = startImpersonation();
        try {
            InputStream result = null;
            Run run = getRunByRefNames(jobId, buildId);
            if (run != null) {
                result = getOctaneLogFile(run);
            } else {
                logger.error("build '" + jobId + " #" + buildId + "' not found");
            }
            return result;
        } finally {
            stopImpersonation(originalContext);
        }
    }

    @Override
    public InputStream getCoverageReport(String jobId, String buildId, String reportFileName) {
        ACLContext originalContext = startImpersonation();
        try {
            InputStream result = null;
            Run run = getRunByRefNames(jobId, buildId);
            if (run != null) {
                File coverageReport = new File(run.getRootDir(), reportFileName);
                if (coverageReport.exists()) {
                    try {
                        result = new FileInputStream(coverageReport);
                    } catch (FileNotFoundException fnfe) {
                        logger.warn("file not found for '" + reportFileName + "' although just verified its existence, concurrency?");
                    }
                }
            } else {
                logger.error("build '" + jobId + " #" + buildId + "' not found");
            }
            return result;
        } finally {
            stopImpersonation(originalContext);
        }
    }

    @Override
    public InputStream getSCMData(String jobId, String buildId) {
        ACLContext originalContext = startImpersonation();
        InputStream result = null;

        try {
            Run run = getRunByRefNames(jobId, buildId);
            if (run != null) {
                result = SCMUtils.getSCMData(run);
            } else {
                logger.error("build '" + jobId + " #" + buildId + "' not found");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to load SCMData for jobId " + jobId + " buildId " + buildId, e);
        } finally {
            stopImpersonation(originalContext);
        }

        return result;
    }

    @Override
    public SSCProjectConfiguration getSSCProjectConfiguration(String jobId, String buildId) {
        ACLContext originalContext = startImpersonation();
        try {
            SSCProjectConfiguration result = null;
            Run run = getRunByRefNames(jobId, buildId);
            SSCServerConfigUtil.SSCProjectVersionPair projectVersionPair = null;

            if (run instanceof AbstractBuild) {
                projectVersionPair = SSCServerConfigUtil.getProjectConfigurationFromBuild((AbstractBuild) run);
            } else if (run instanceof WorkflowRun) {
                projectVersionPair = SSCServerConfigUtil.getProjectConfigurationFromWorkflowRun((WorkflowRun) run);
            } else {
                logger.error("build '" + jobId + " #" + buildId + "' (of specific type AbstractBuild or WorkflowRun) not found");
                return result;
            }

            String sscServerUrl = SSCServerConfigUtil.getSSCServer();
            String sscAuthToken = getFortifySSCToken();

            if (sscServerUrl != null && !sscServerUrl.isEmpty() && projectVersionPair != null) {
                result = dtoFactory.newDTO(SSCProjectConfiguration.class)
                        .setSSCUrl(sscServerUrl)
                        .setSSCBaseAuthToken(sscAuthToken)
                        .setProjectName(projectVersionPair.project)
                        .setProjectVersion(projectVersionPair.version);
            }

            return result;
        } finally {
            stopImpersonation(originalContext);
        }
    }

    private String getFortifySSCToken(){
        OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(getInstanceId());
        FortifySSCTokenParameter parameter = (FortifySSCTokenParameter) octaneClient.getConfigurationService().getConfiguration().getParameter(FortifySSCTokenParameter.KEY);
        if (parameter != null) {
            return parameter.getToken();
        }
        return "";
    }

    @Override
    public Long getFodRelease(String jobId, String buildId) {
        ACLContext originalContext = startImpersonation();
        try {
            Run run = getRunByRefNames(jobId, buildId);
            if (run instanceof AbstractBuild) {
                return FodConfigUtil.getFODReleaseFromBuild((AbstractBuild) run);
            } else {
                logger.error("build '" + jobId + " #" + buildId + "' (of specific type AbstractBuild) not found");
                return null;
            }
        } finally {
            stopImpersonation(originalContext);
        }
    }

    @Override
    public FodServerConfiguration getFodServerConfiguration() {

        ACLContext originalContext = startImpersonation();
        try {

            FodConfigUtil.ServerConnectConfig fodServerConfig = FodConfigUtil.getFODServerConfig();
            if (fodServerConfig != null) {
                return dtoFactory.newDTO(FodServerConfiguration.class)
                        .setClientId(fodServerConfig.clientId)
                        .setClientSecret(fodServerConfig.clientSecret)
                        .setApiUrl(fodServerConfig.apiUrl)
                        .setBaseUrl(fodServerConfig.baseUrl);
            }
            return null;
        } finally {
            stopImpersonation(originalContext);
        }
    }

    @Override
    public void runTestDiscovery(DiscoveryInfo discoveryInfo) {
        ACLContext securityContext = startImpersonation();
        try {
            TestExecutionJobCreatorService.runTestDiscovery(discoveryInfo);
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public PipelineNode createExecutor(DiscoveryInfo discoveryInfo) {
        if (EntityConstants.Executors.UFT_TEST_RUNNER_SUBTYPE_ENTITY_NAME.equals(discoveryInfo.getExecutorType())) {
            ACLContext securityContext = startImpersonation();
            try {
                Job project = TestExecutionJobCreatorService.createExecutor(discoveryInfo);
                return ModelFactory.createStructureItem(project);
            } finally {
                stopImpersonation(securityContext);
            }
        } else {
            return null;
        }
    }

    @Override
    public OctaneResponse checkRepositoryConnectivity(TestConnectivityInfo testConnectivityInfo) {
        ACLContext securityContext = startImpersonation();
        try {
            OctaneResponse response;
            OctaneClient octaneClient = OctaneSDK.getClientByInstanceId(getInstanceId());
            if (ConfigurationParameterFactory.isUftTestConnectionDisabled(octaneClient.getConfigurationService().getConfiguration())) {
                logger.info("checkRepositoryConnectivity : validation disabled");
                response = DTOFactory.getInstance().newDTO(OctaneResponse.class).setStatus(HttpStatus.SC_OK);
            } else {
                response = ExecutorConnectivityService.checkRepositoryConnectivity(testConnectivityInfo);
            }

            //validate UftTestRunnerFolderParameter
            if (response.getStatus() == HttpStatus.SC_OK) {
                UftTestRunnerFolderParameter uftFolderParameter = (UftTestRunnerFolderParameter) octaneClient.getConfigurationService()
                        .getConfiguration().getParameter(UftTestRunnerFolderParameter.KEY);
                if (uftFolderParameter != null) {
                    List<String> errors = new ArrayList<>();
                    ConfigurationValidator.checkUftFolderParameter(uftFolderParameter, errors);
                    if (!errors.isEmpty()) {
                        response.setStatus(HttpStatus.SC_BAD_REQUEST);
                        response.setBody(errors.get(0));
                    }
                }
            }
            return response;
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public void deleteExecutor(String id) {
        ACLContext securityContext = startImpersonation();
        try {
            UftJobRecognizer.deleteDiscoveryJobByExecutor(id);
            UftJobRecognizer.deleteExecutionJobByExecutorIfNeverExecuted(id);
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public OctaneResponse upsertCredentials(CredentialsInfo credentialsInfo) {
        ACLContext securityContext = startImpersonation();
        try {
            return ExecutorConnectivityService.upsertRepositoryCredentials(credentialsInfo);
        } finally {
            stopImpersonation(securityContext);
        }
    }

    @Override
    public List<CredentialsInfo> getCredentials() {
        List<StandardUsernameCredentials> list = CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, (Item) null, null, (DomainRequirement) null);
        List<CredentialsInfo> output = list.stream()
                .map(c -> dtoFactory.newDTO(CredentialsInfo.class).setCredentialsId(c.getId()).setUsername(CredentialsNameProvider.name(c)))
                .collect(Collectors.toList());
        return output;
    }

    private ACLContext startImpersonation() {
        return startImpersonation(null);
    }

    private ACLContext startImpersonation(Long workspaceId) {
        return ImpersonationUtil.startImpersonation(getInstanceId(), workspaceId);
    }

    private void stopImpersonation(ACLContext impersonatedContext) {
        ImpersonationUtil.stopImpersonation(impersonatedContext);
    }

    private PipelineNode createPipelineNode(String name, Job job, boolean includeParameters) {
        PipelineNode tmpConfig = dtoFactory.newDTO(PipelineNode.class)
                .setJobCiId(JobProcessorFactory.getFlowProcessor(job).getTranslatedJobName())
                .setName(name);

        if (includeParameters) {
            tmpConfig.setParameters(ParameterProcessors.getConfigs(job));

            //setIsTestRunner
            if (tmpConfig.getParameters() != null) {
                Optional opt = tmpConfig.getParameters().stream().filter(p -> TestsToRunConverterBuilder.TESTS_TO_RUN_PARAMETER.equals(p.getName())).findFirst();
                tmpConfig.setIsTestRunner(opt.isPresent());
            }

            //setHasUpstream
            if (job instanceof AbstractProject) {
                List<AbstractProject> upstreams = Jenkins.get().getDependencyGraph().getUpstream((AbstractProject) job);
                tmpConfig.setHasUpstream(upstreams.size() > 0);
            }

        }
        return tmpConfig;
    }

    private PipelineNode createPipelineNodeFromJobName(String name) {
        return dtoFactory.newDTO(PipelineNode.class)
                .setJobCiId(BuildHandlerUtils.translateFolderJobName(name))
                .setName(name);
    }

    private CIBuildStatusInfo getUnavailableJobStatus(String ciJobId, String paramName, String paramValue) {
        return DTOFactory.getInstance().newDTO(CIBuildStatusInfo.class)
                .setBuildStatus(CIBuildStatus.UNAVAILABLE)
                .setJobCiId(ciJobId)
                .setParamName(paramName)
                .setParamValue(paramValue)
                .setResult(CIBuildResult.UNAVAILABLE);
    }

    private void addParametersAndDefaultBranchFromConfig(Item item, PipelineNode result) {
        String defaultBranchesConfig = RunnerMiscSettingsGlobalConfiguration.getInstance() != null ? RunnerMiscSettingsGlobalConfiguration.getInstance().getDefaultBranches() : null;
        if(defaultBranchesConfig != null && !defaultBranchesConfig.isEmpty()) {
            String[] defaultBranchesArray = defaultBranchesConfig.split(DEFAULT_BRANCHES_SEPARATOR);
            Set<String> defaultBranches = Arrays.stream(defaultBranchesArray)
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());

            Collection<? extends Job> allJobs = item.getAllJobs();

            Job job = allJobs.stream()
                    .filter(tempJob -> defaultBranches.contains(getDisplayNameFromJob(tempJob)))
                    .findFirst().orElse(null);

            if (job != null) {
                String defaultBranch = getDisplayNameFromJob(job);
                result.setParameters(ParameterProcessors.getConfigs(job))
                        .setDefaultBranchName(defaultBranch);
            }
        }
    }

    private String getDisplayNameFromJob(Job tempJob) {
        return tempJob.getDisplayName() != null ? tempJob.getDisplayName() : tempJob.getName();
    }

    private InputStream getOctaneLogFile(Run run) {
        InputStream result = null;
        String octaneLogFilePath = run.getRootDir() + File.separator + "octane_log";
        File logFile = new File(octaneLogFilePath);
        if (!logFile.exists()) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(logFile);
                 InputStream logStream = run.getLogInputStream();
                 PlainTextConsoleOutputStream out = new PlainTextConsoleOutputStream(fileOutputStream)) {
                IOUtils.copy(logStream, out);
                out.flush();
            } catch (IOException ioe) {
                logger.error("failed to transfer native log to Octane's one for " + run);
            }
        }
        try {
            result = new FileInputStream(octaneLogFilePath);
        } catch (IOException ioe) {
            logger.error("failed to obtain log for " + run);
        }
        return result;
    }

    private Run getRunByRefNames(String jobId, String buildId) {
        Run result = null;
        Job project = getJobByRefId(jobId);
        if (project != null) {
            result = project.getBuildByNumber(Integer.parseInt(buildId));
        }
        return result;
    }

    private void doRunImpl(Job job, CIParameters ciParameters) {
        AbstractProjectProcessor jobProcessor = JobProcessorFactory.getFlowProcessor(job);
        doRunStopImpl(jobProcessor::scheduleBuild, "execution", job, ciParameters);
    }

    private void doStopImpl(Job job, CIParameters ciParameters) {
        AbstractProjectProcessor jobProcessor = JobProcessorFactory.getFlowProcessor(job);
        doRunStopImpl(jobProcessor::cancelBuild, "stop", job, ciParameters);
    }

    //  TODO: the below flow should go via JobProcessor, once scheduleBuild will be implemented for all of them
    private void doRunStopImpl(BiConsumer<Cause, ParametersAction> method, String methodName, Job job, CIParameters ciParameters) {
        ParametersAction parametersAction = new ParametersAction();
        if (ciParameters != null) {
            parametersAction = new ParametersAction(createParameters(job, ciParameters));
        }

        Cause cause = new Cause.RemoteCause(ConfigurationService.getSettings(getInstanceId()) == null ? "non available URL" :
                ConfigurationService.getSettings(getInstanceId()).getLocation(), "octane driven " + methodName);
        method.accept(cause, parametersAction);
    }

    public static List<ParameterValue> createParameters(Job project, CIParameters ciParameters) {
        List<ParameterValue> result = new ArrayList<>();
        boolean parameterHandled;
        ParameterValue tmpValue;
        ParametersDefinitionProperty paramsDefProperty = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);

        if (paramsDefProperty == null) {
            paramsDefProperty = new ParametersDefinitionProperty();
        }

        Map<String, CIParameter> ciParametersMap = ciParameters.getParameters().stream().collect(Collectors.toMap(CIParameter::getName, Function.identity()));
        for (ParameterDefinition paramDef : paramsDefProperty.getParameterDefinitions()) {
            parameterHandled = false;
            CIParameter ciParameter = ciParametersMap.remove(paramDef.getName());
            if (ciParameter != null) {
                tmpValue = null;
                switch (ciParameter.getType()) {
                    case NUMBER:
                    case STRING:
                        tmpValue = new StringParameterValue(ciParameter.getName(), ciParameter.getValue().toString());
                        break;
                    case BOOLEAN:
                        tmpValue = new BooleanParameterValue(ciParameter.getName(), Boolean.parseBoolean(ciParameter.getValue().toString()));
                        break;
                    case PASSWORD:
                        tmpValue = new PasswordParameterValue(ciParameter.getName(), ciParameter.getValue().toString());
                        break;
                    default:
                        break;
                }
                if (tmpValue != null) {
                    result.add(tmpValue);
                    parameterHandled = true;
                }
            }
            if (!parameterHandled) {
                if (paramDef instanceof FileParameterDefinition) {
                    File file = new File("");
                    try {
                        try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
                            outputStream.write(new byte[0]);
                        }
                    } catch (IOException ioe) {
                        logger.error("failed to create default value for file parameter '" + paramDef.getName() + "'", ioe);
                    }
                    tmpValue = new FileParameterValue(paramDef.getName(), file, file.getName());
                    result.add(tmpValue);


                } else {
                    result.add(paramDef.getDefaultParameterValue());
                }
            }
        }

        //add parameters that are not defined in job
        for (CIParameter notDefinedParameter : ciParametersMap.values()) {
            tmpValue = new StringParameterValue(notDefinedParameter.getName(), notDefinedParameter.getValue().toString());
            result.add(tmpValue);
        }
        return result;
    }

    private Job getJobByRefId(String jobName) {
        Item item = getItemByRefId(jobName);
        return item instanceof Job ? (Job) item : null;
    }

    private Item getItemByRefId(String jobName) {
        String myJobName = BuildHandlerUtils.revertTranslateFolderJobName(jobName);
        Item item = Jenkins.get().getItemByFullName(myJobName);
        if (item == null) {
            // defect #875099 : two jobs with the same name in folder - are not treated correctly
            // PATCH UNTIL OCTANE SEND jobRefId correctly (fix in octane : pipeline-management-add-dialog-controller.js)
            //bug in octane : duplicating parent prefix, for example job f1/f2/jobA , appear as f1/f2/f1/f2/jobA
            //try to reduce duplication and find  job

            int jobNameIndex = myJobName.lastIndexOf('/');
            if (jobNameIndex > 0) {
                String parentPrefix = myJobName.substring(0, jobNameIndex);
                String notDuplicatedParentPrefix1 = myJobName.substring(0, parentPrefix.length() / 2);
                String notDuplicatedParentPrefix2 = myJobName.substring((parentPrefix.length() / 2) + 1, jobNameIndex);
                if (StringUtils.equals(notDuplicatedParentPrefix1, notDuplicatedParentPrefix2)) {
                    String alternativeJobName = notDuplicatedParentPrefix1 + myJobName.substring(jobNameIndex);
                    item = Jenkins.get().getItemByFullName(alternativeJobName);
                }
            }
        }

        return item;
    }

    public static File getAllowedStorageFile() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        File folder;
        if (jenkins != null) {
            folder = getAllowedStorageFileForMasterJenkins(jenkins);
        } else {/*is slave*/
            folder =  new File("octanePluginContent");
        }
        return folder;
    }

    private static File getAllowedStorageFileForMasterJenkins(Jenkins jenkins) {
        boolean allowPrint;
        synchronized (skipOctaneAllowedStoragePrintLock) {
            //do allowPrint only once
            allowPrint = !skipOctaneAllowedStoragePrint;
            skipOctaneAllowedStoragePrint = true;
        }

        File folder;
        // jenkins.xml
        //  <arguments>-Xrs -Xmx256m -octaneAllowedStorage=userContentTemp -Dhudson.lifecycle=hudson.lifecycle.WindowsServiceLifecycle -jar "%BASE%\jenkins.war"
        String prop = System.getProperty("octaneAllowedStorage");
        if (StringUtils.isNotEmpty(prop)) {
            folder = new File(prop);
            if (!folder.isAbsolute()) {
                folder = new File(jenkins.getRootDir(), prop);
            }
            if (allowPrint) {
                systemLogger.info("octaneAllowedStorage : " + folder.getAbsolutePath());
                //validate that folder exist
                if (!folder.exists() && !folder.mkdirs()) {
                    systemLogger.warning("Failed to create octaneAllowedStorage : " + folder.getAbsolutePath() + ". Create this folder and restart Jenkins.");
                }
            }
        } else {
            folder = new File(jenkins.getRootDir(), "userContent");
        }
        return folder;
    }

    public static CIServerInfo getJenkinsServerInfo() {
        CIServerInfo result = dtoFactory.newDTO(CIServerInfo.class);
        String serverUrl = Jenkins.get().getRootUrl();
        if (serverUrl != null && serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        result.setType(CIServerTypes.JENKINS.value())
                .setVersion(Jenkins.VERSION)
                .setUrl(serverUrl)
                .setSendingTime(System.currentTimeMillis());
        return result;
    }

    public static void publishEventToRelevantClients(CIEvent event) {
        OctaneSDK.getClients().forEach(c->c.getEventsService().publishEvent(event));
    }

    @Override
    public String getParentJobName(String jobId) {
        if (jobId != null && jobId.contains(BuildHandlerUtils.JOB_LEVEL_SEPARATOR)) {
            int index = jobId.lastIndexOf(BuildHandlerUtils.JOB_LEVEL_SEPARATOR);
            String parent = jobId.substring(0, index);
            return parent;
        }
        return null;
    }
}