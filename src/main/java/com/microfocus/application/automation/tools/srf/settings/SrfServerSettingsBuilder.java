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

package com.microfocus.application.automation.tools.srf.settings;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.*;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.microfocus.application.automation.tools.srf.utilities.SrfClient;
import com.microfocus.application.automation.tools.srf.utilities.SrfTrustManager;
import hudson.*;
import hudson.model.*;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

/**
 * Created by shepshel on 20/07/2016.
 */
public class SrfServerSettingsBuilder extends Builder {
    private String credentialsId;
    private String srfServerName;
    private String srfProxyName;
    private String srfTunnelPath;

    @DataBoundConstructor
    public SrfServerSettingsBuilder(String credentialsId, String srfServerName, String srfProxyName, String srfTunnelPath) {
        this.credentialsId = credentialsId;
        this.srfServerName = srfServerName;
        this.srfProxyName = srfProxyName;
        this.srfTunnelPath = srfTunnelPath;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsID(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getSrfServerName() {
        return srfServerName;
    }

    public void setSrfServerName(String srfServerName) {
        this.srfServerName = srfServerName;
    }

    public String getSrfProxyName() {
        return srfProxyName;
    }

    public void setSrfProxyName(String srfProxyName) {
        this.srfProxyName = srfProxyName;
    }

    public String getSrfTunnelPath() {
        return srfTunnelPath;
    }

    public void setSrfTunnelPath(String srfTunnelPath) {
        this.srfTunnelPath = srfTunnelPath;
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
        private String srfServerName;
        private String srfProxyName;
        private String credentialsId;
        private String srfTunnelPath;

        public SrfDescriptorImpl() {
            super(SrfServerSettingsBuilder.class);
            load();
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        public String getCredentialsId() {
            return credentialsId;
        }

        public void setCredentialsID(String credentialsId) {
            this.credentialsId = credentialsId;
        }

        public void setSrfServerName(String srfServerName) {
            this.srfServerName = srfServerName;
        }

        public String getSrfServerName() {
            return srfServerName;
        }

        public String getSrfProxyName() {
            return srfProxyName;
        }

        public void setSrfProxyName(String srfProxyName) {
            this.srfProxyName = srfProxyName;
        }

        public String getSrfTunnelPath() {
            return srfTunnelPath;
        }

        public void setSrfTunnelPath(String srfTunnelPath) {
            this.srfTunnelPath = srfTunnelPath;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "";
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
                this.credentialsId = srfCommon.getString("credentialsId");
                this.srfProxyName = srfCommon.getString("srfProxyName");
                this.srfServerName = srfCommon.getString("srfServerName");
                this.srfTunnelPath = srfCommon.getString("srfTunnelPath");

                req.bindJSON(SrfServerSettingsBuilder.class, srfCommon);

                save();
            } catch (Exception e){
                System.err.println(e);
            }

            return super.configure(req, formData);
        }

        public FormValidation doCheckSrfServerName(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.ok();
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

        public FormValidation doTestConnection(@AncestorInPath Item item,
                                               @QueryParameter("srfServerName") final String srfServer,
                                               @QueryParameter("srfProxyName") final String srfProxyName,
                                               @QueryParameter("credentialsId") final String credentialsId) throws IOException, ServletException {

            if (StringUtils.isBlank(srfServer)) {
                return FormValidation.error("Missing SRF's server URL");
            }

            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                SrfTrustManager _trustMgr = new SrfTrustManager();
                sslContext.init(null, new SrfTrustManager[]{_trustMgr}, null);
                SSLContext.setDefault(sslContext);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                URL proxy = null;
                if (srfProxyName != null && !srfProxyName.isEmpty()){
                    proxy = new URL(srfProxyName);
                }

                UsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
                                Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
                        CredentialsMatchers.withId(credentialsId));


                SrfClient srfClient = new SrfClient(srfServer, sslSocketFactory, proxy);

                srfClient.login(credentials.getUsername(), credentials.getPassword().getPlainText());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                return FormValidation.error("Connection error: connection initialization error " + e.getMessage());
            } catch (UnknownHostException e) {
                return FormValidation.error("Connection error: Unknown host " + e.getMessage());
            } catch (AuthenticationException e) {
                return FormValidation.error("Authentication error: " + e.getMessage());
            } catch (SSLHandshakeException e) {
                return FormValidation.error("Connection error: " + e.getMessage() + " (Could be a proxy issue)");
            } catch (SocketTimeoutException e) {
                return FormValidation.error("Connection error: Timed out request");
            } catch (Exception e) {
                return FormValidation.error("Connection error: " + e.getMessage());
            }

            return FormValidation.ok("Success");
        }

        /**
         * To fill in the credentials drop down list which's field is 'credentialsId'.
         * This method's name works with tag <c:select/>.
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context,
                                                     @QueryParameter String credentialsId) {
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeAs(
                            ACL.SYSTEM,
                            context,
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.create().build())
                    .includeCurrentValue(credentialsId);
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item project,
                                                   @QueryParameter String url,
                                                   @QueryParameter String value) {

            value = Util.fixEmptyAndTrim(value);
            if (value == null) {
                return FormValidation.ok();
            }

            url = Util.fixEmptyAndTrim(url);
            if (url == null)
            // not set, can't check
            {
                return FormValidation.ok();
            }

            if (url.indexOf('$') >= 0)
            // set by variable, can't check
            {
                return FormValidation.ok();
            }

            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    project,
                    project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                    URIRequirementBuilder.create().build(),
                    new IdMatcher(value))) {

                if (StringUtils.equals(value, o.value)) {
                    return FormValidation.ok();
                }
            }
            // no credentials available, can't check
            return FormValidation.warning("Cannot find any credentials with id " + value);
        }
    }
}
