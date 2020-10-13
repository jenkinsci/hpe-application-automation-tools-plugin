/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;
import java.util.Map;

import static com.microfocus.application.automation.tools.octane.configuration.ReflectionUtils.getFieldValue;

/***
 * A utility class to help retrieving the configuration of the FOD
 * in Jenkins: URL, connection params, releaseId etc.
 */
public class FodConfigUtil {
    private final static Logger logger = SDKBasedLoggerProvider.getLogger(FodConfigUtil.class);

    public final static String FOD_DESCRIPTOR = "org.jenkinsci.plugins.fodupload.FodGlobalDescriptor";
    public final static String FOD_STATIC_ASSESSMENT_STEP = "org.jenkinsci.plugins.fodupload.StaticAssessmentBuildStep";

    public static class ServerConnectConfig {
        public String baseUrl;
        public String apiUrl;
        public String clientId;
        public String clientSecret;

    }

    public static ServerConnectConfig getFODServerConfig() {
        Descriptor fodDescriptor = getFODDescriptor();
        ServerConnectConfig serverConnectConfig = null;
        if (fodDescriptor != null) {
            serverConnectConfig = new ServerConnectConfig();
            serverConnectConfig.apiUrl = getFieldValue(fodDescriptor, "apiUrl");
            serverConnectConfig.baseUrl = getFieldValue(fodDescriptor, "baseUrl");
            serverConnectConfig.clientId = getFieldValue(fodDescriptor, "clientId");
            serverConnectConfig.clientSecret = getFieldValue(fodDescriptor, "clientSecret");
        }
        return serverConnectConfig;
    }

    private static Descriptor getFODDescriptor() {
        return Jenkins.getInstanceOrNull().getDescriptorByName(FOD_DESCRIPTOR);

    }

    public static Long getFODReleaseFromBuild(AbstractBuild build) {
        return build != null ? getRelease(build.getProject()) : null;
    }

    public static Long getFODReleaseFromRun(WorkflowRun run) {
        // to understand what does it mean for pipeline run
        logger.debug("implement get release for " + run );
        return null;
    }

    private static Long getRelease(AbstractProject project) {
        Long release = getReleaseVersionBefore12(project);
        if(release != null){
            logger.debug("A Version before 12 is detected.");
            return release;
        }

        release = getReleaseVersion12(project);
        if(release != null) {
            logger.debug("A Version 12 or higher is detected.");
        }
        logger.debug("No release was set to this job");
        return release;
    }
    private static Long getReleaseVersion12(AbstractProject project){
        for (Object publisher : project.getPublishersList()) {
            if (publisher instanceof Publisher &&
                    FOD_STATIC_ASSESSMENT_STEP.equals(publisher.getClass().getName())) {
                return getReleaseByReflectionV12(publisher);
            }
        }
        return null;
    }
    private static Long getReleaseVersionBefore12(AbstractProject project){
        for (Object publisher : project.getPublishersList()) {
            if (publisher instanceof Publisher &&
                    FOD_STATIC_ASSESSMENT_STEP.equals(publisher.getClass().getName())) {
                return getReleaseByReflection(publisher);
            }
        }
        return null;
    }
    private static Long getReleaseByReflectionV12(Object fodPublisher) {

        Object sharedBuildStep = getFieldValue(fodPublisher, "sharedBuildStep");
        if(sharedBuildStep == null){
            return null;
        }
        return getReleaseByReflection(sharedBuildStep);
    }
    private static Long getReleaseByReflection(Object fodPublisher) {

        Object modelObj = getFieldValue(fodPublisher, "model");
        if (modelObj == null) {
            return null;
        }
        String bsiToken = getFieldValue(modelObj, "bsiTokenOriginal");
        return parseBSITokenAndGetReleaseId(bsiToken);
    }

    private static Long parseBSITokenAndGetReleaseId(String bsiToken) {
        try {
            return handleURLFormat(bsiToken);
        } catch (Exception e) {
            return handleBase64Format(bsiToken);
        }
    }

    private static Long handleBase64Format(String bsiToken) {

        String bsi64 = StringUtils.newStringUtf8(Base64.decodeBase64(bsiToken));
        try {
            Map bsiJsonAsMap = new ObjectMapper().readValue(bsi64,
                    TypeFactory.defaultInstance().constructType(Map.class));
            return Long.valueOf(bsiJsonAsMap.get("releaseId").toString());

        } catch (IOException e) {
            logger.error("failed to read the BSI token base64:" + e.getMessage());
            return null;
        }

    }

    private static Long handleURLFormat(String bsiToken) {
        //https://api.sandbox.fortify.com/bsi2.aspx?tid=159&tc=Octane&pv=3059&payloadType=ANALYSIS_PAYLOAD&astid=25&ts=JS%2fXML%2fHTML
        if (bsiToken == null) {
            return null;
        }

        int pvIndex = bsiToken.indexOf("pv");
        String releaseString = bsiToken.substring(bsiToken.indexOf('=', pvIndex) + 1, bsiToken.indexOf('&', pvIndex));
        return Long.valueOf(releaseString);
    }
}
