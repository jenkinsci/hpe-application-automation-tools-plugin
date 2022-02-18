/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.run;

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
import net.minidev.json.JSONObject;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Locale;
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
        // get the UFT Mobile server settings
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
            out.println("Failed to upload app to UFT Mobile server. Cause: UFT Mobile url didn't be configured.");
            return false;
        } else {
            mcServerUrl = mcServerSettingsModel.getProperties().getProperty("MobileHostAddress");
            out.println(String.format("There are %d apps to be uploaded.", paths.size()));
            String workspace = build.getWorkspace() == null ? "" : build.getWorkspace().toURI().getPath();

            for (int i = 1; i <= paths.size(); i++) {
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

                try {
                    out.println(String.format("starting to upload app %d %s", i, originPath));
                    app = job.upload(mcServerUrl, uploadAppModel.getAuthModel(), uploadAppModel.getProxySettings(), path);
                    if (app == null) {
                        if (uploadAppModel.isUseProxy()) {
                            out.println(String.format("Failed to upload app, Cause UFT Mobile connection info is incorrect. url:%s, Proxy url:%s",
                                    mcServerUrl, uploadAppModel.getProxySettings().getFsProxyAddress()));
                        } else if (uploadAppModel.isUseAuthentication()) {
                            out.println(String.format("Failed to upload app, Cause UFT Mobile connection info is incorrect. url:%s, Proxy url:%s, proxy userName:%s",
                                    mcServerUrl, uploadAppModel.getProxySettings().getFsProxyAddress(), uploadAppModel.getProxySettings().getFsProxyUserName()));
                        } else {
                            out.println(String.format("Failed to upload app, Cause UFT Mobile connection info is incorrect. url:%s.", mcServerUrl));
                        }
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
                    out.println(String.format("Failed to upload app to UFT Mobile server. Cause: File: %s is not found.", path));
                    build.setResult(Result.FAILURE);
                    allSuccess = false;
                    continue;
                } catch (IOException ioe) {
                    Util.displayIOException(ioe, listener);
                    build.setResult(Result.FAILURE);
                    allSuccess = false;
                    continue;
                } catch (InterruptedException e) {
                    build.setResult(Result.ABORTED);
                    return false;
                } catch (Exception e) {
                    if (uploadAppModel.isUseProxy()) {
                        out.println(String.format("Failed to upload app, Cause UFT Mobile connection info is incorrect. url:%s, Proxy url:%s",
                                mcServerUrl, uploadAppModel.getProxySettings().getFsProxyAddress()));
                    } else if (uploadAppModel.isUseAuthentication()) {
                        out.println(String.format("Failed to upload app, Cause UFT Mobile connection info is incorrect. url:%s, Proxy url:%s, proxy userName:%s",
                                mcServerUrl, uploadAppModel.getProxySettings().getFsProxyAddress(), uploadAppModel.getProxySettings().getFsProxyUserName()));
                    } else {
                        out.println(String.format("Failed to upload app, Cause UFT Mobile connection info is incorrect. url:%s", mcServerUrl));
                    }
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

            return "Upload app to UFT Mobile Server";
        }

        public boolean hasMCServers() {
            return MCServerSettingsGlobalConfiguration.getInstance().hasMCServers();
        }

        public MCServerSettingsModel[] getMcServers() {
            return MCServerSettingsGlobalConfiguration.getInstance().getInstallations();
        }
    }
}
