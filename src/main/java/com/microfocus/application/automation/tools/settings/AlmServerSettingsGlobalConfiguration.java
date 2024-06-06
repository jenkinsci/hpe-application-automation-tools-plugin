/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.settings;

import com.microfocus.application.automation.tools.model.AlmServerSettingsModel;
import com.microfocus.application.automation.tools.model.CredentialsModel;
import com.microfocus.application.automation.tools.model.SSOCredentialsModel;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.XmlFile;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Extension(ordinal = 1)
public class AlmServerSettingsGlobalConfiguration extends GlobalConfiguration implements Serializable {

    public AlmServerSettingsGlobalConfiguration(){
        load();
    }

    public static AlmServerSettingsGlobalConfiguration getInstance() {
        return GlobalConfiguration.all().get(AlmServerSettingsGlobalConfiguration.class);
    }

    @Override
    protected XmlFile getConfigFile() {
        XmlFile xmlFile = super.getConfigFile();
        ConfigurationMigrationUtil.migrateConfigurationFileIfRequired(xmlFile,
                "com.microfocus.application.automation.tools.settings.AlmServerSettingsBuilder.xml",
                "AlmServerSettingsBuilder_-DescriptorImpl",
                "AlmServerSettingsGlobalConfiguration");

        return xmlFile;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        List<AlmServerSettingsModel> models = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();

        Object data = formData.get("inst");
        if (data instanceof JSONObject) {
            jsonArray.add(data);
        } else if (data instanceof JSONArray) {
            jsonArray.addAll((JSONArray) data);
        }

        for (Object jsonObject : jsonArray) {
            JSONObject json = (JSONObject) jsonObject;
            Object credentialsObj = json.get("credentials");
            List<CredentialsModel> credentials = new ArrayList<>();
            if (credentialsObj instanceof JSONArray) {
                JSONArray credentialsObjArray = (JSONArray) credentialsObj;
                credentials = req.bindJSONToList(CredentialsModel.class, credentialsObjArray);
            } else if (credentialsObj instanceof JSONObject) {
                CredentialsModel credentialsModel = req.bindJSON(CredentialsModel.class, (JSONObject) credentialsObj);
                credentials.add(credentialsModel);
            }

            Object ssoCredentialsObj = json.get("ssoCredentials");
            List<SSOCredentialsModel> ssoCredentials = new ArrayList<>();
            if (ssoCredentialsObj instanceof JSONArray) {
                JSONArray ssoCredentialsObjArray = (JSONArray) ssoCredentialsObj;
                ssoCredentials = req.bindJSONToList(SSOCredentialsModel.class, ssoCredentialsObjArray);
            } else if (ssoCredentialsObj instanceof JSONObject) {
                SSOCredentialsModel ssoCredentialsModel = req.bindJSON(SSOCredentialsModel.class, (JSONObject) ssoCredentialsObj);
                ssoCredentials.add(ssoCredentialsModel);
            }

            AlmServerSettingsModel newModel = req.bindJSON(AlmServerSettingsModel.class, json);
            newModel.set_almCredentials(credentials);
            newModel.set_almSSOCredentials(ssoCredentials);
            if (!StringUtils.isEmpty(newModel.getAlmServerName()) && !(StringUtils.isEmpty(newModel.getAlmServerUrl()))) {
                models.add(newModel);
            }
        }

        setInstallations(models.toArray(new AlmServerSettingsModel[0]));

        save();

        return super.configure(req, formData);
    }

    @RequirePOST
    public FormValidation doCheckAlmServerUrl(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        return checkQcServerURL(value, false);
    }

    @CopyOnWrite
    private AlmServerSettingsModel[] installations = new AlmServerSettingsModel[0];

    public AlmServerSettingsModel[] getInstallations() {
        return installations;
    }

    public void setInstallations(AlmServerSettingsModel... installations) {
        this.installations = installations;
    }

    public FormValidation doCheckAlmServerName(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.error("ALM server name cannot be empty");
        }

        return FormValidation.ok();
    }

    private FormValidation checkQcServerURL(String value, Boolean acceptEmpty) {
        String url;
        // Path to the page to check if the server is alive
        String page = "servlet/tdservlet/TDAPI_GeneralWebTreatment";

        // Do will allow empty value?
        if (StringUtils.isBlank(value)) {
            if (!acceptEmpty) {
                return FormValidation.error("ALM server must be defined");
            } else {
                return FormValidation.ok();
            }
        }

        // Does the URL ends with a "/" ? if not, add it
        if (value.lastIndexOf('/') == value.length() - 1) {
            url = value + page;
        } else {
            url = value + "/" + page;
        }

        // Open the connection and perform a HEAD request
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) ProxyConfiguration.open(new URL(url));
            connection.setRequestMethod("GET");

            // Check the response code
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return FormValidation.error(connection.getResponseMessage());
            }
        } catch (MalformedURLException ex) {
            // This is not a valid URL
            return FormValidation.error("ALM server URL is malformed.");
        } catch (IOException ex) {
            // Cant open connection to the server
            return FormValidation.error("Error opening a connection to the ALM server");
        }

        return FormValidation.ok();
    }

    public FormValidation doCheckAlmUsername(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.error("Username must be set");
        }

        return FormValidation.ok();
    }

    public FormValidation doCheckAlmClientID(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.error("Client ID must be set");
        }

        return FormValidation.ok();
    }

    public Boolean hasAlmServers() {
        return installations.length > 0;
    }
}
