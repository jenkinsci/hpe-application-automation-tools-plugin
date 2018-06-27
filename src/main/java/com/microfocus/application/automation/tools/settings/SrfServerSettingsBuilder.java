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

package com.microfocus.application.automation.tools.settings;

import com.microfocus.application.automation.tools.srf.model.SrfException;
import com.microfocus.application.automation.tools.srf.model.SrfServerSettingsModel;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

/**
 * Created by shepshel on 20/07/2016.
 */
public class SrfServerSettingsBuilder extends Builder{

        public SrfServerSettingsBuilder(){

        }

        @Override
        public SrfServerSettingsBuilder.SrfDescriptorImpl getDescriptor() {
            return (SrfServerSettingsBuilder.SrfDescriptorImpl) super.getDescriptor();
        }

        /**
         * Descriptor for {@link SrfServerSettingsBuilder}. Used as a singleton. The class is marked as
         * public so that it can be accessed from views.
         *
         * <p>
         * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the
         * actual HTML fragment for the configuration screen.
         */
        @Extension
        // This indicates to Jenkins that this is an implementation of an extension
        // point.
        public static final class SrfDescriptorImpl extends BuildStepDescriptor<Builder> {
            private static String srfServer;


            @Override
            public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
                // Indicates that this builder can be used with all kinds of project
                // types
                return true;
            }

            /**
             * This human readable name is used in the configuration screen.
             */
            @Override
            public String getDisplayName() {
                return "";
            }

            public SrfDescriptorImpl() {
                load();
            }
            public static String getSrfServerName() {
                return srfServer;
            }
            @Override
            public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
                // To persist global configuration information,
                // set that to properties and call save().
                // useFrench = formData.getBoolean("useFrench");
                // ^Can also use req.bindJSON(this, formData);
                // (easier when there are many fields; need set* methods for this,
                // like setUseFrench)
                // req.bindParameters(this, "locks.");

                try {
                    JSONObject srfCommon = formData.getJSONObject("SrfCommon");
                    setInstallations(req.bindJSONToList(SrfServerSettingsModel.class, srfCommon.get("SRF_123")).toArray(
                            new SrfServerSettingsModel[0]));

                    save();
                } catch (Exception e){
                    // Ignore
                }

                return super.configure(req, formData);
            }

            @CopyOnWrite
            private SrfServerSettingsModel[] installations = new SrfServerSettingsModel[0];

            public SrfServerSettingsModel[] getInstallations() {
                return installations;
            }

            public void setInstallations(SrfServerSettingsModel... installations)  {
                this.installations = installations;
            }

            public FormValidation doCheckSrfServerName(@QueryParameter String value) {
                if (StringUtils.isBlank(value)) {
                    return FormValidation.error("Srf server name cannot be empty");
                }

                try {
                    URL urlUnderValidation = new URL(value);
                    return FormValidation.ok();
                }
                catch (MalformedURLException e) {
                    return FormValidation.error(String.format("SRF server url is invalid: %s", e.getMessage()));
                }
            }

            public FormValidation doCheckSrfProxyName(@QueryParameter String value) {
                if (StringUtils.isBlank(value))
                    return FormValidation.ok();

                try {
                    URL urlUnderValidation = new URL(value);
                    return FormValidation.ok();
                }
                catch (MalformedURLException e) {
                    return FormValidation.error(String.format("SRF proxy server url is invalid: %s", e.getMessage()));
                }
            }

            public FormValidation doCheckSrfAppName(@QueryParameter String value) {
                if (StringUtils.isBlank(value))
                    return FormValidation.error("Srf client id cannot be empty");

                return FormValidation.ok();
            }

            public FormValidation doCheckSrfSecretName(@QueryParameter String value) {
                if (StringUtils.isBlank(value))
                    return FormValidation.error("Srf client secret cannot be empty");

                return FormValidation.ok();
            }

            public FormValidation doTestConnection(@QueryParameter("srfServerName") final String srfServer,
                                                   @QueryParameter("srfProxyName") final String srfProxyName,
                                                   @QueryParameter("srfAppName") final String srfAppName,
                                                   @QueryParameter("srfSecretName") final String srfSecretName) throws IOException, ServletException {

                HttpURLConnection connection;
                BufferedReader in = null;

                try {
                    URL url = new URL(srfServer.concat("/health/ready"));
                    if (srfProxyName != null && !srfProxyName.isEmpty()) {
                        URL proxyUrl = new URL(srfProxyName);
                        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));
                        connection = (HttpURLConnection) url.openConnection(proxy);
                    } else {
                        connection = (HttpURLConnection) url.openConnection();
                    }

                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();

                    in = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }

                    if (responseCode >= 300)
                        throw new SrfException("Received response code of: " + responseCode);

                    return FormValidation.ok("Success");
                } catch (UnknownHostException e) {
                    return FormValidation.error("Connection error : Unknown host " + e.getMessage());
                } catch (Exception e) {
                    return FormValidation.error("Connection error : " + e.getMessage());
                } finally {
                    if (in != null)
                        in.close();
                }
            }

            @JavaScriptMethod
            public Boolean hasSrfServers() {
                return installations.length > 0;
            }
        }
}
