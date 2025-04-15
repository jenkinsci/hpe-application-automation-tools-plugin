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
package com.microfocus.application.automation.tools.octane.configuration;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.Publisher;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
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
            serverConnectConfig.clientSecret = retrieveSecretDecryptedValue(getFieldValue(fodDescriptor, "clientSecret"));
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
        // BSI Token is being deprecated, try to get releaseId directly first then fallback to BSI Token parsing
        Long release = getReleaseId(project);
        if (release != null) {
            return release;
        } else {
            logger.debug("Falling back to retrieving release from BSI Token");
        }

        release = getReleaseVersionBefore12(project);
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

    private static Long getReleaseId(AbstractProject project){
        for (Object publisher : project.getPublishersList()) {
            if (publisher instanceof Publisher &&
                    FOD_STATIC_ASSESSMENT_STEP.equals(publisher.getClass().getName())) {
                Object sharedBuildStep = getFieldValue(publisher, "sharedBuildStep");
                if (sharedBuildStep != null) {
                    logger.debug(sharedBuildStep.toString());
                    return getReleaseIdByReflection(sharedBuildStep);
                } else {
                    return getReleaseIdByReflection(publisher);
                }
            }
        }
        return null;
    }
    private static Long getReleaseIdByReflection(Object fodPublisher) {

        Object modelObj = getFieldValue(fodPublisher, "model");
        if (modelObj == null) {
            return null;
        }
        String releaseId = getFieldValue(modelObj, "releaseId");
        if (releaseId != null) {
            return Long.valueOf(releaseId);
        } else {
            logger.debug("Unable to find releaseId directly");
            return null;
        }
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

    //

    public static String decrypt(String stringToDecrypt) {
        Secret decryptedSecret = Secret.decrypt(stringToDecrypt);
        return  decryptedSecret != null ?  decryptedSecret.getPlainText() : stringToDecrypt;
    }

    public static String decrypt(Secret stringToDecrypt) {
        return stringToDecrypt.getPlainText();
    }

    public static String encrypt(String stringToEncrypt) {
        String result = stringToEncrypt;
        if(Secret.decrypt(stringToEncrypt) == null){
            result = Secret.fromString(stringToEncrypt).getEncryptedValue();
        }
        return result;
    }

    public static boolean isEncrypted(String stringToEncrypt) {
        return (Secret.decrypt(stringToEncrypt) != null);
    }

    public static boolean isCredential(String id) {
        StringCredentials s = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StringCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        null,
                        null
                ),
                CredentialsMatchers.allOf(
                        CredentialsMatchers.withId(id)
                )
        );
        return (s != null);
    }

    public static String retrieveSecretDecryptedValue(String id) {
        StringCredentials s = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StringCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        null,
                        null
                ),
                CredentialsMatchers.allOf(
                        CredentialsMatchers.withId(id)
                )
        );
        return s != null ? decrypt(s.getSecret()) : id;
    }

}
