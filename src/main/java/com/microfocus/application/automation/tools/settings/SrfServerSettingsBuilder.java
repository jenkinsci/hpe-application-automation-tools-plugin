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

package com.microfocus.application.automation.tools.settings;

import com.microfocus.application.automation.tools.srf.model.SrfServerSettingsModel;
import com.microfocus.application.automation.tools.srf.utilities.SrfClient;
import com.microfocus.application.automation.tools.srf.utilities.SrfTrustManager;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

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
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    SrfTrustManager _trustMgr = new SrfTrustManager();
                    sslContext.init(null, new SrfTrustManager[]{_trustMgr}, null);
                    SSLContext.setDefault(sslContext);
                    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                    SrfClient srfClient = new SrfClient(srfServer, sslSocketFactory , new URL(srfProxyName));
                    srfClient.login(srfAppName, srfSecretName);
                } catch (KeyManagementException | NoSuchAlgorithmException e) {
                    return FormValidation.error("Connection error : connection initialization error " + e.getMessage());
                } catch (UnknownHostException e) {
                    return FormValidation.error("Connection error : Unknown host " + e.getMessage());
                } catch (AuthenticationException e) {
                    return FormValidation.error("Authentication error: " + e.getMessage());
                } catch (SSLHandshakeException e) {
                    return FormValidation.error("Connection error : " + e.getMessage() + " (Could be a proxy issue)");
                } catch (Exception e) {
                    return FormValidation.error("Connection error : " + e.getMessage());
                }

                return FormValidation.ok("Success");
            }

            @JavaScriptMethod
            public Boolean hasSrfServers() {
                return installations.length > 0;
            }
        }
}
