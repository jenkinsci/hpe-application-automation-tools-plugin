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
package com.microfocus.application.automation.tools.settings;

import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.sv.svconfigurator.core.impl.jaxb.ServerInfo;
import com.microfocus.sv.svconfigurator.core.impl.processor.Credentials;
import com.microfocus.sv.svconfigurator.serverclient.ICommandExecutor;
import com.microfocus.sv.svconfigurator.serverclient.impl.CommandExecutorFactory;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.XmlFile;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Extension(ordinal = 1)
public class SvServerSettingsGlobalConfiguration extends GlobalConfiguration implements Serializable {

    public static SvServerSettingsGlobalConfiguration getInstance() {
        return GlobalConfiguration.all().get(SvServerSettingsGlobalConfiguration.class);
    }

    @Override
    protected XmlFile getConfigFile() {
        XmlFile xmlFile = super.getConfigFile();
        ConfigurationMigrationUtil.migrateConfigurationFileIfRequired(xmlFile,
                "com.microfocus.application.automation.tools.settings.SvServerSettingsBuilder.xml",
                "SvServerSettingsBuilder_-DescriptorImpl",
                "SvServerSettingsGlobalConfiguration");

        return xmlFile;
    }

    @CopyOnWrite
    private SvServerSettingsModel[] servers;

    public SvServerSettingsGlobalConfiguration() {
        load();
    }

    private static boolean isHttpsSchema(String url) {
        try {
            return "https".equals(new URL(url).getProtocol());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        List<SvServerSettingsModel> list = req.bindJSONToList(SvServerSettingsModel.class, formData.get("srv"));

        validateMandatoryFields(list);

        servers = list.toArray(new SvServerSettingsModel[list.size()]);

        save();

        return super.configure(req, formData);
    }

    private void validateMandatoryFields(List<SvServerSettingsModel> servers) throws FormException {
        for (SvServerSettingsModel server : servers) {
            validateConfiguration(doCheckName(server.getName()), "name");
            validateConfiguration(doCheckUrl(server.getUrl()), "url");
            validateConfiguration(doCheckUsername(server.getUsername(), server.getUrl()), "username");
            validateConfiguration(doCheckPassword(server.getPassword(), server.getUrl()), "password");
        }
    }

    private void validateConfiguration(FormValidation result, String formField) throws FormException {
        if (!result.equals(FormValidation.ok())) {
            throw new FormException("Validation of property in Service Virtualization server configuration failed: " + result.getMessage(), formField);
        }
    }

    public FormValidation doCheckUrl(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.error("Management Endpoint URL cannot be empty");
        }
        try {
            new URL(value);
        } catch (MalformedURLException e) {
            return FormValidation.error("'" + value + "' is not valid URL");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckUsername(@QueryParameter String value, @QueryParameter("url") final String url) {
        if (isHttpsSchema(url) && StringUtils.isBlank(value)) {
            return FormValidation.error("Username is required for secured server");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckPassword(@QueryParameter String value, @QueryParameter("url") final String url) {
        if (isHttpsSchema(url) && StringUtils.isBlank(value)) {
            return FormValidation.error("Password is required for secured server");
        }
        return FormValidation.ok();
    }

    @RequirePOST
    @SuppressWarnings("unused")
    public FormValidation doTestConnection(@QueryParameter("url") final String url,
                                           @QueryParameter("trustEveryone") final boolean trustEveryone,
                                           @QueryParameter("username") final String username,
                                           @QueryParameter("password") final String password) {
        try {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            Credentials credentials = (!StringUtils.isBlank(username)) ? new Credentials(username, password) : null;
            ICommandExecutor commandExecutor = new CommandExecutorFactory().createCommandExecutor(new URL(url), trustEveryone, credentials);
            ServerInfo serverInfo = commandExecutor.getClient().getServerInfo();
            return FormValidation.ok("Validation passed. Connected to %s server of version: %s", serverInfo.getServerType(), serverInfo.getProductVersion());
        } catch (Exception e) {
            return FormValidation.error("Validation failed: " + e.getMessage());
        }
    }

    public SvServerSettingsModel[] getServers() {
        return servers;
    }

    public void setServers(SvServerSettingsModel[] servers) {
        this.servers = servers;
    }

    public FormValidation doCheckName(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.error("Name cannot be empty");
        }

        return FormValidation.ok();
    }
}
