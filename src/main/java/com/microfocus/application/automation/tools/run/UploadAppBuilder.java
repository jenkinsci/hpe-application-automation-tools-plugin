/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.run;

import com.microfocus.application.automation.tools.mc.JobConfigurationProxy;
import com.microfocus.application.automation.tools.model.MCServerSettingsModel;
import com.microfocus.application.automation.tools.model.ProxySettings;
import com.microfocus.application.automation.tools.model.UploadAppModel;
import com.microfocus.application.automation.tools.model.UploadAppPathModel;
import com.microfocus.application.automation.tools.settings.MCServerSettingsBuilder;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.minidev.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
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
    public UploadAppBuilder(String mcServerName, String mcUserName, String mcPassword, String mcTenantId, ProxySettings proxySettings, List<UploadAppPathModel> applicationPaths) {
        uploadAppModel = new UploadAppModel(mcServerName, mcUserName, mcPassword, mcTenantId, proxySettings, applicationPaths);
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        // get the mc server settings
        MCServerSettingsModel mcServerSettingsModel = getMCServerSettingsModel();
        JobConfigurationProxy job = JobConfigurationProxy.getInstance();
        JSONObject app = null;
        String mcServerUrl = "";
        PrintStream out = listener.getLogger();
        List<UploadAppPathModel> paths = null;
        if(uploadAppModel != null){
            paths = uploadAppModel.getApplicationPaths();
        }
        boolean allSuccess = true;
        if(mcServerSettingsModel == null){
            out.println("Failed to upload app to MC server. Cause: MC url didn't be configured." );
            return false;
        }else{
            mcServerUrl = mcServerSettingsModel.getProperties().getProperty("MobileHostAddress");
            out.println(String.format("There are %d apps to be uploaded.", paths.size()));
            String workspace = new File(build.getWorkspace().toURI()).getCanonicalPath();
            for(int i=1; i<=paths.size(); i++){
                String path = paths.get(i-1).getMcAppPath();
                if(StringUtils.isNullOrEmpty(path)){
                    out.println(String.format("ignore the empty app %d upload", i));
                    continue;
                }
                //case insensitive replace of workspace to its real path
                path = path.replaceAll("(?i)"+ Pattern.quote("${WORKSPACE}"), Matcher.quoteReplacement(workspace));
                try{
                    out.println(String.format("starting to upload app %d %s", i, path));
                    if(uploadAppModel.getProxySettings() == null){
                        app = job.upload(mcServerUrl, uploadAppModel.getMcUserName(),uploadAppModel.getMcPassword(), uploadAppModel.getMcTenantId(), null, null, null, path);
                    }else{
                        app = job.upload(mcServerUrl, uploadAppModel.getMcUserName(),uploadAppModel.getMcPassword(), uploadAppModel.getMcTenantId(), uploadAppModel.getProxySettings().getFsProxyAddress(),uploadAppModel.getProxySettings().getFsProxyUserName(), uploadAppModel.getProxySettings().getFsProxyPassword(),path);
                    }
                    if(app == null){
                        if(uploadAppModel.isUseProxy()){
                            out.println(String.format("Failed to upload app, Cause MC connection info is incorrect. url:%s, username:%s, Proxy url:%s",
                                    mcServerUrl, uploadAppModel.getMcUserName(), uploadAppModel.getProxySettings().getFsProxyAddress()));
                        }else if(uploadAppModel.isUseAuthentication()){
                            out.println(String.format("Failed to upload app, Cause MC connection info is incorrect. url:%s, username:%s, Proxy url:%s, proxy userName:%s",
                                    mcServerUrl, uploadAppModel.getMcUserName(), uploadAppModel.getProxySettings().getFsProxyAddress(), uploadAppModel.getProxySettings().getFsProxyUserName()));
                        }else{
                            out.println(String.format("Failed to upload app, Cause MC connection info is incorrect. url:%s, username:%s,",
                                    mcServerUrl, uploadAppModel.getMcUserName()));
                        }
                        build.setResult(Result.FAILURE);
                        return false;
                    }
                    if((Boolean)app.get("error")){
                        out.println("Job failed because got error message during the application uploading. " + app.toJSONString());
                        allSuccess = false;
                        build.setResult(Result.FAILURE);
                    }
                    out.println("uploaded app info: " + app.toJSONString());
                } catch(FileNotFoundException fnf){
                    out.println(String.format("Failed to upload app to MC server. Cause: File: %s is not found.", path));
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
                } catch (Exception e){
                    if(uploadAppModel.isUseProxy()){
                        out.println(String.format("Failed to upload app, Cause MC connection info is incorrect. url:%s, username:%s, Proxy url:%s",
                                mcServerUrl, uploadAppModel.getMcUserName(), uploadAppModel.getProxySettings().getFsProxyAddress()));
                    }else if(uploadAppModel.isUseAuthentication()){
                        out.println(String.format("Failed to upload app, Cause MC connection info is incorrect. url:%s, username:%s, Proxy url:%s, proxy userName:%s",
                                mcServerUrl, uploadAppModel.getMcUserName(), uploadAppModel.getProxySettings().getFsProxyAddress(), uploadAppModel.getProxySettings().getFsProxyUserName()));
                    }else{
                        out.println(String.format("Failed to upload app, Cause MC connection info is incorrect. url:%s, username:%s,",
                                mcServerUrl, uploadAppModel.getMcUserName()));
                    }
                    build.setResult(Result.FAILURE);
                    return false;
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

            return "Upload app to Mobile Center";
        }

        public boolean hasMCServers() {
            return Hudson.getInstance().getDescriptorByType(
                    MCServerSettingsBuilder.MCDescriptorImpl.class).hasMCServers();
        }

        public MCServerSettingsModel[] getMcServers() {
            return Hudson.getInstance().getDescriptorByType(
                    MCServerSettingsBuilder.MCDescriptorImpl.class).getInstallations();
        }
    }
}
