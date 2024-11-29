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
package com.microfocus.application.automation.tools.run;

import com.microfocus.application.automation.tools.mc.Constants;
import com.microfocus.application.automation.tools.mc.JobConfigurationProxy;
import com.microfocus.application.automation.tools.model.*;
import com.microfocus.application.automation.tools.settings.MCServerSettingsGlobalConfiguration;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: jingwei
 * Date: 5/17/16
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadAppBuilder extends Builder {

    private final UploadAppModel uploadAppModel;

    @DataBoundConstructor
    public UploadAppBuilder(String mcServerName, AuthModel authModel, ProxySettings proxySettings, List<UploadAppPathModel> applicationPaths) {
        uploadAppModel = new UploadAppModel(mcServerName, authModel, proxySettings, applicationPaths);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        // get the Digital Lab server settings
        MCServerSettingsModel mcServerSettingsModel = getMCServerSettingsModel();
        JobConfigurationProxy job = JobConfigurationProxy.getInstance();
        JSONObject app = null;
        String mcServerUrl = "";
        PrintStream out = listener.getLogger();
        List<UploadAppPathModel> paths = null;
        if (uploadAppModel != null) {
            paths = uploadAppModel.getApplicationPaths();
        }
        boolean allSuccess = true;
        if (mcServerSettingsModel == null) {
            out.println("Failed to upload app to Digital Lab server. Cause: Digital Lab URL was not configured.");
            return false;
        } else {
            mcServerUrl = mcServerSettingsModel.getProperties().getProperty("MobileHostAddress");
            Map<String, String> headers = job.login(mcServerUrl, uploadAppModel.getAuthModel(), uploadAppModel.getProxySettings());
            if (headers == null || headers.size() == 0) {
                if (uploadAppModel.isUseProxy()) {
                    out.println(String.format("Failed to upload app, Cause Digital Lab connection info is incorrect. url:%s, Proxy url:%s",
                            mcServerUrl, uploadAppModel.getProxySettings().getFsProxyAddress()));
                } else if (uploadAppModel.isUseAuthentication()) {
                    out.println(String.format("Failed to upload app, Cause Digital Lab connection info is incorrect. url:%s, Proxy url:%s, proxy userName:%s",
                            mcServerUrl, uploadAppModel.getProxySettings().getFsProxyAddress(), uploadAppModel.getProxySettings().getFsProxyUserName()));
                } else {
                    out.println(String.format("Failed to upload app, Cause Digital Lab connection info is incorrect. url:%s", mcServerUrl));
                }
                build.setResult(Result.FAILURE);
                return false;
            }

            if(paths == null || paths.size() == 0) {
                return true;
            }

            out.println(String.format("There are %d apps to be uploaded.", paths.size()));
            String workspace = build.getWorkspace() == null ? "" : build.getWorkspace().toURI().getPath();

            for (int i = 1; i <= paths.size(); i++) {
                String appUploadWorkspace = paths.get(i - 1).getMcAppWorkspace();
                String path = paths.get(i - 1).getMcAppPath();
                String originPath = path;
                if (StringUtils.isNullOrEmpty(path)) {
                    out.println(String.format("ignore the empty app %d upload", i));
                    continue;
                }
                //case insensitive replace of workspace to its real path
                if (path.toUpperCase(Locale.ENGLISH).startsWith("${WORKSPACE}")) {
                    path = path.replaceAll("(?i)" + Pattern.quote("${WORKSPACE}"), Matcher.quoteReplacement(workspace));
                    if (!FilenameUtils.normalize(path).startsWith(FilenameUtils.normalize(workspace))) {
                        out.println(String.format("Failed to upload app, Cause invalid application file: %s", path));
                        build.setResult(Result.FAILURE);
                        allSuccess = false;
                        continue;
                    }
                }
                FilePath filePath = new FilePath(build.getWorkspace().getChannel(), path);
                File tempFile = null;
                if (filePath.isRemote()) {
                    tempFile = File.createTempFile("uftm", "." + FilenameUtils.getExtension(path));
                    try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                        filePath.copyTo(outputStream);
                        path = tempFile.getCanonicalPath();
                    } catch (NoSuchFileException noSuchFileException) {
                        out.println(String.format("Failed to upload app, Cause cannot find application file: %s", path));
                        build.setResult(Result.FAILURE);
                        allSuccess = false;
                        continue;
                    } catch (Exception e) {
                        out.println(String.format("Failed to upload app, Cause failed to copy application file: %s", path));
                        build.setResult(Result.FAILURE);
                        allSuccess = false;
                        continue;
                    }
                }
                //check workspace exist or not in MC server
                String appUploadWorkspaceName = "";
                if(!StringUtils.isNullOrEmpty(appUploadWorkspace)){
                    JSONObject result = job.isWorkspaceExist(headers, mcServerUrl, uploadAppModel.getProxySettings(), appUploadWorkspace);
                    if(result == null || (result != null && (!result.containsKey("uuid") || !result.getAsString("uuid").equals(appUploadWorkspace)))){
                        out.println(String.format("Failed to upload app %d %s, Cause cannot find target workspace id: %s", i, originPath, appUploadWorkspace));
                        build.setResult(Result.FAILURE);
                        allSuccess = false;
                        continue;
                    }else{
                        appUploadWorkspaceName = result.getAsString("name");
                    }
                }else{
                    appUploadWorkspaceName = Constants.SHARED_ASSETS;
                }
                //upload app
                try {
                    out.println(String.format("starting to upload app %d %s to workspace %s", i, originPath, appUploadWorkspaceName));
                    app = job.upload(headers, mcServerUrl, uploadAppModel.getProxySettings(), path, appUploadWorkspace);
                    if (app == null) {
                        out.println("Failed to upload app.");
                        build.setResult(Result.FAILURE);
                        return false;
                    }
                    if ((Boolean) app.get("error")) {
                        out.println("Job failed because got error message during the application uploading. " + app.toJSONString());
                        allSuccess = false;
                        build.setResult(Result.FAILURE);
                    }
                    out.println("uploaded app info: " + app.toJSONString());
                } catch (FileNotFoundException fnf) {
                    out.println(String.format("Failed to upload app to Digital Lab server. Cause: File: %s is not found.", path));
                    build.setResult(Result.FAILURE);
                    allSuccess = false;
                    continue;
                } catch (IOException ioe) {
                    Util.displayIOException(ioe, listener);
                    build.setResult(Result.FAILURE);
                    allSuccess = false;
                    continue;
                } catch (Exception e) {
                    out.println("Failed to upload app.");
                    build.setResult(Result.FAILURE);
                    return false;
                } finally {
                    if (tempFile != null) {
                        Files.delete(tempFile.toPath());
                    }
                }
            }
        }
        return allSuccess;
    }

    public MCServerSettingsModel getMCServerSettingsModel() {
        for (MCServerSettingsModel mcServer : getDescriptor().getMcServers()) {
            if (this.uploadAppModel != null
                    && uploadAppModel.getMcServerName().equals(mcServer.getMcServerName())) {
                return mcServer;
            }
        }
        return null;
    }

    public UploadAppModel getUploadAppModel() {
        return uploadAppModel;
    }

    // This indicates to Jenkins that this is an implementation of an extension point
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {

            load();
        }

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {

            return true;
        }

        @Override
        public String getDisplayName() {

            return "Upload app to Digital Lab (formerly UFT Mobile) Server";
        }

        public boolean hasMCServers() {
            return MCServerSettingsGlobalConfiguration.getInstance().hasMCServers();
        }

        public MCServerSettingsModel[] getMcServers() {
            return MCServerSettingsGlobalConfiguration.getInstance().getInstallations();
        }

        /**
         * Gets all valid workspace list.
         *
         * @param mcUrl the server name
         * @return the mc workspace list
         */
        @SuppressWarnings("squid:S2259")
        @JavaScriptMethod
        public JSONArray getValidWorkspaces(String mcUrl, String authType, String mcUserName, String mcPassword, String mcTenantId, String mcExecToken,
                                                      boolean useProxy, String proxyAddress, boolean useAuthentication, String proxyUserName, String proxyPassword) {
            JSONArray workspaces = null;
            for (MCServerSettingsModel mcServer : this.getMcServers()) {
                if (!StringUtils.isNullOrEmpty(mcUrl)
                        && mcUrl.equals(mcServer.getMcServerName())) {
                    mcUrl = mcServer.getMcServerUrl();
                }
            }
            AuthModel authModel = new AuthModel(mcUserName, mcPassword, mcTenantId, mcExecToken, authType);
            ProxySettings proxySettings =new ProxySettings(useAuthentication, proxyAddress, proxyUserName, proxyPassword);
            try {
                JobConfigurationProxy job = JobConfigurationProxy.getInstance();
                workspaces = job.getAllValidWorkspaces(mcUrl, authModel, proxySettings);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return changeResult(workspaces);
        }

        private JSONArray changeResult(JSONArray workspaces){
            JSONArray result = new JSONArray();
            if (workspaces != null) {
                for (int i = 0; i < workspaces.size(); i++) {
                    JSONObject workspace = (JSONObject) workspaces.get(i);
                    if(workspace.getAsString("name").equals(Constants.SHARED_ASSETS)){
                        result.add(workspace);
                    }
                }
                for (int i = 0; i < workspaces.size(); i++) {
                    JSONObject workspace = (JSONObject) workspaces.get(i);
                    if(!workspace.getAsString("name").equals(Constants.SHARED_ASSETS)){
                        result.add(workspace);
                    }
                }
            }
            return result;
        }

    }
}
