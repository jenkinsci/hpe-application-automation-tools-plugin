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

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hp.octane.integrations.exceptions.OctaneConnectivityException;
import com.hp.octane.integrations.services.configurationparameters.FortifySSCTokenParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.utils.OctaneUrlParser;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.CIJenkinsServicesImpl;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationListener;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationValidator;
import com.microfocus.application.automation.tools.octane.configuration.SDKBasedLoggerProvider;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Octane configuration settings
 */
@Extension(ordinal = 1)
public class OctaneServerSettingsGlobalConfiguration extends GlobalConfiguration implements Serializable {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(OctaneServerSettingsGlobalConfiguration.class);

    public static OctaneServerSettingsGlobalConfiguration getInstance() {
        return GlobalConfiguration.all().get(OctaneServerSettingsGlobalConfiguration.class);
    }

    @CopyOnWrite
    private volatile OctaneServerSettingsModel[] servers;

    private transient Map<String, OctaneConfiguration> octaneConfigurations = new HashMap<>();

    @Override
    protected XmlFile getConfigFile() {
        XmlFile xmlFile = super.getConfigFile();
        ConfigurationMigrationUtil.migrateConfigurationFileIfRequired(xmlFile,
                "com.microfocus.application.automation.tools.settings.OctaneServerSettingsBuilder.xml",
                "OctaneServerSettingsBuilder_-OctaneDescriptorImpl",
                "OctaneServerSettingsGlobalConfiguration");

        return xmlFile;
    }

    public OctaneServerSettingsGlobalConfiguration() {
        load();
        convertFortifyParameters();

        if (servers == null) {
            servers = new OctaneServerSettingsModel[0];
        }

        boolean shouldSave = false;
        // defense for non-octane users.Previously, before multi-configuration, octane had only one configuration,
        // even empty. So after moving to multi-configuration, non-octane users have non-valid configuration and
        // will fail to save jenkins configuration as missing valid octane configuration
        if (servers.length == 1 && StringUtils.isEmpty(servers[0].getUiLocation())) {
            servers = new OctaneServerSettingsModel[0];
            shouldSave = true;
        }

        //  upgrade flow to add internal ID to configuration
        for (OctaneServerSettingsModel server : servers) {
            if (server.getInternalId() == null || server.getInternalId().isEmpty()) {
                server.setInternalId(UUID.randomUUID().toString());
                shouldSave = true;
            }
        }
        if (shouldSave) {
            save();
        }
    }

    private void convertFortifyParameters() {
        boolean updated = false;
        if (servers != null) {
            for (OctaneServerSettingsModel model : servers) {
                if (convertFortifyParameters(model)) {
                    updated = true;
                }
            }
            if (updated) {
                save();
            }
        }
    }

    private boolean convertFortifyParameters(OctaneServerSettingsModel model) {
        if (!model.isFortifyParamsConverted()) {
            if (model.getSscBaseToken() != null && !model.getSscBaseToken().trim().isEmpty()) {
                String params = StringUtils.isEmpty(model.getParameters()) ? "" : model.getParameters().trim() + System.lineSeparator();

                params += FortifySSCTokenParameter.KEY + ":" + model.getSscBaseToken().trim();
                model.setParameters(params);
            }
            model.setFortifyParamsConverted(true);
            return true;
        }
        return false;
    }

    public void initOctaneClients() {
        if (servers.length > 0) {
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, servers.length));
            for (OctaneServerSettingsModel innerServerConfiguration : servers) {
                OctaneConfiguration octaneConfiguration = OctaneConfiguration.createWithUiLocation(innerServerConfiguration.getIdentity(), innerServerConfiguration.getUiLocation());
                octaneConfiguration.setClient(innerServerConfiguration.getUsername());
                octaneConfiguration.setSecret(innerServerConfiguration.getPassword().getPlainText());
                octaneConfiguration.setSuspended(innerServerConfiguration.isSuspend());
                octaneConfiguration.setImpersonatedUser(innerServerConfiguration.getImpersonatedUser());
                octaneConfiguration.clearParameters();
                innerServerConfiguration.getParametersAsMap().entrySet().forEach(entry -> {
                    ConfigurationParameterFactory.addParameter(octaneConfiguration, entry.getKey(), entry.getValue());
                });

                octaneConfigurations.put(innerServerConfiguration.getInternalId(), octaneConfiguration);
                executor.execute(() -> {
                    OctaneSDK.addClient(octaneConfiguration, CIJenkinsServicesImpl.class);
                });
            }
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        Object data = formData.get("mqm");
        JSONArray jsonArray = new JSONArray();
        if (data instanceof JSONObject) {
            jsonArray.add(data);
        } else if (data instanceof JSONArray) {
            jsonArray.addAll((JSONArray) data);
        }

        handleDeletedConfigurations(jsonArray);
        for (Object jsonObject : jsonArray) {
            JSONObject json = (JSONObject) jsonObject;
            OctaneServerSettingsModel newModel = req.bindJSON(OctaneServerSettingsModel.class, json);
            OctaneServerSettingsModel oldModel;

            String internalId = json.getString("internalId");
            validateConfiguration(doCheckUiLocation(json.getString("uiLocation"), internalId), "Location");
            oldModel = getSettingsByInternalId(internalId);
            if (oldModel != null) {
                validateConfiguration(doCheckInstanceId(newModel.getIdentity()), "Plugin instance id");
                newModel.setInternalId(internalId);
            }

            setModel(newModel);
        }

        return super.configure(req, formData);
    }

    private void handleDeletedConfigurations(JSONArray jsonArray) {
        if (servers == null) {
            return;
        }

        Set<String> foundInternalId = jsonArray.stream().map(jsonObj -> ((JSONObject) jsonObj).getString("internalId")).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        Set<OctaneServerSettingsModel> serversToRemove = Arrays.stream(servers).filter(server -> !foundInternalId.contains(server.getInternalId())).collect(Collectors.toSet());

        if (!serversToRemove.isEmpty()) {
            List<OctaneServerSettingsModel> serversToLeave = new ArrayList<>(Arrays.asList(servers));
            for (OctaneServerSettingsModel serverToRemove : serversToRemove) {
                logger.info("Removing client with instance Id: " + serverToRemove.getIdentity());
                serversToLeave.remove(serverToRemove);
                octaneConfigurations.remove(serverToRemove.getInternalId());


                try {
                    OctaneSDK.removeClient(OctaneSDK.getClientByInstanceId(serverToRemove.getIdentity()));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    //failed to remove from SDK
                    //just remove from jenkins
                    logger.warn("Failed to remove client with instance Id: " + serverToRemove.getIdentity() + " from SDK : " + e.getMessage());
                }
            }

            servers = serversToLeave.toArray(new OctaneServerSettingsModel[0]);
            save();
        }
    }

    public void setModel(OctaneServerSettingsModel newModel) {
        //infer uiLocation

        try {
            OctaneUrlParser octaneUrlParser = ConfigurationValidator.parseUiLocation(newModel.getUiLocation());
            newModel.setSharedSpace(octaneUrlParser.getSharedSpace());
            newModel.setLocation(octaneUrlParser.getLocation());
        } catch (FormValidation fv) {
            logger.warn("tested configuration failed on Octane URL parse: " + fv.getMessage(), fv);
        }

        OctaneServerSettingsModel oldModel = getSettingsByInternalId(newModel.getInternalId());
        //  set identity in new model
        if (oldModel == null) {
            if (newModel.getIdentity() == null || newModel.getIdentity().isEmpty()) {
                newModel.setIdentity(UUID.randomUUID().toString());
            }
            if (newModel.getIdentityFrom() == null) {
                newModel.setIdentityFrom(System.currentTimeMillis());
            }
        } else if (oldModel.getIdentity() != null && !oldModel.getIdentity().isEmpty()) {
            newModel.setIdentityFrom(oldModel.getIdentityFrom());
        }

        OctaneServerSettingsModel[] serversBackup = servers;
        if (oldModel == null) {
            if (servers == null) {
                servers = new OctaneServerSettingsModel[]{newModel};
            } else {
                if (servers.length == 1 && !servers[0].isValid()) {
                    //  replacing the first dummy one
                    servers[0] = newModel;
                } else {
                    //  adding new one
                    OctaneServerSettingsModel[] newServers = new OctaneServerSettingsModel[servers.length + 1];
                    System.arraycopy(servers, 0, newServers, 0, servers.length);
                    newServers[servers.length] = newModel;
                    servers = newServers;
                }
            }
        } else {
            removeClientIfIdentityChanged(servers, newModel, oldModel);
        }
        OctaneConfiguration octaneConfiguration = octaneConfigurations.containsKey(newModel.getInternalId()) ?
                octaneConfigurations.get(newModel.getInternalId()) :
                OctaneConfiguration.create(newModel.getIdentity(), newModel.getLocation(), newModel.getSharedSpace());
        octaneConfiguration.setUrlAndSpace(newModel.getLocation(), newModel.getSharedSpace());
        octaneConfiguration.setClient(newModel.getUsername());
        octaneConfiguration.setSecret(newModel.getPassword().getPlainText());
        octaneConfiguration.setImpersonatedUser(newModel.getImpersonatedUser());
        octaneConfiguration.setSuspended(newModel.isSuspend());

        octaneConfiguration.clearParameters();
        newModel.getParametersAsMap().entrySet().forEach(entry -> {
            ConfigurationParameterFactory.addParameter(octaneConfiguration, entry.getKey(), entry.getValue());
        });


        if (!octaneConfigurations.containsValue(octaneConfiguration)) {
            octaneConfigurations.put(newModel.getInternalId(), octaneConfiguration);
            try {
                OctaneSDK.addClient(octaneConfiguration, CIJenkinsServicesImpl.class);
            } catch (Exception e) {
                servers = serversBackup;
                throw e;
            }
        }

        if (!newModel.equals(oldModel)) {
            fireOnChanged(newModel, oldModel);
        }

        save();
    }

    private void removeClientIfIdentityChanged(OctaneServerSettingsModel[] servers, OctaneServerSettingsModel newModel, OctaneServerSettingsModel oldModel) {
        if (servers != null) {
            for (int i = 0; i < servers.length; i++) {
                if (newModel.getInternalId().equals(servers[i].getInternalId())) {
                    servers[i] = newModel;
                    if (!newModel.getIdentity().equals(oldModel.getIdentity())) {
                        logger.info("Removing client with instance Id: " + oldModel.getIdentity());
                        OctaneSDK.removeClient(OctaneSDK.getClientByInstanceId(octaneConfigurations.get(oldModel.getInternalId()).getInstanceId()));
                        octaneConfigurations.remove(newModel.getInternalId());
                    }
                    break;
                }
            }
        }
    }

    private void fireOnChanged(OctaneServerSettingsModel newConf, OctaneServerSettingsModel oldConf) {
        //resetJobListCache
        try {
            OctaneSDK.getClientByInstanceId(newConf.getIdentity()).getTasksProcessor().resetJobListCache();
        } catch (Exception e) {
            logger.info("Failed to resetJobListCache for client with instance Id: " + newConf.getIdentity() + ", " + e.getMessage());
        }

        //resetOctaneRootsCache
        try {
            OctaneSDK.getClientByInstanceId(newConf.getIdentity()).getConfigurationService().resetOctaneRootsCache();
        } catch (Exception e) {
            logger.info("Failed to resetOctaneRootsCache for client with instance Id: " + newConf.getIdentity() + ", " + e.getMessage());
        }

        //update listeners
        ExtensionList<ConfigurationListener> listeners = ExtensionList.lookup(ConfigurationListener.class);
        for (ConfigurationListener listener : listeners) {
            try {
                listener.onChanged(newConf, oldConf);
            } catch (ThreadDeath t) {
                throw t;
            } catch (Exception t) {
                logger.warn(t);
            }
        }
    }

    @RequirePOST
    @SuppressWarnings("unused")
    public FormValidation doTestConnection(StaplerRequest req,
                                           @QueryParameter("uiLocation") String uiLocation,
                                           @QueryParameter("username") String username,
                                           @QueryParameter("password") String password,
                                           @QueryParameter("impersonatedUser") String impersonatedUser,
                                           @QueryParameter("suspend") Boolean isSuspend,
                                           @QueryParameter("workspace2ImpersonatedUserConf") String workspace2ImpersonatedUserConf,
                                           @QueryParameter("parameters") String parameters
    ) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        String myImpersonatedUser = StringUtils.trim(impersonatedUser);
        String myUsername = StringUtils.trim(username);
        OctaneUrlParser octaneUrlParser;
        try {
            octaneUrlParser = ConfigurationValidator.parseUiLocation(StringUtils.trim(uiLocation));
        } catch (FormValidation fv) {
            logger.warn("tested configuration failed on Octane URL parse: " + fv.getMessage(), fv);
            return fv;
        }
        logger.info("test configuration to : " + octaneUrlParser.getLocation() + "?p=" + octaneUrlParser.getSharedSpace());


        //  if parse is good, check authentication/authorization
        List<String> fails = new ArrayList<>();
        ConfigurationValidator.checkImpersonatedUser(fails, myImpersonatedUser);
        ConfigurationValidator.checkHoProxySettins(fails);
        List<Entity> availableWorkspaces = ConfigurationValidator.checkConfiguration(fails, octaneUrlParser.getLocation(), octaneUrlParser.getSharedSpace(), myUsername, Secret.fromString(password));

        Map<Long, String> workspace2ImpersonatedUser = ConfigurationValidator.checkWorkspace2ImpersonatedUserConf(workspace2ImpersonatedUserConf, availableWorkspaces, myImpersonatedUser, fails);
        ConfigurationValidator.checkParameters(parameters, myImpersonatedUser, workspace2ImpersonatedUser, fails);

        String suspendMessage = "Note that current configuration is disabled (see in Advanced section)";
        if (fails.isEmpty()) {
            String msg = Messages.ConnectionSuccess();


            if (availableWorkspaces != null && !availableWorkspaces.isEmpty()) {
                int workspaceNumberLimit = 30;
                String titleNewLine = "&#xA;";
                String suffix = (availableWorkspaces.size() > workspaceNumberLimit) ? titleNewLine + "and more " + (availableWorkspaces.size() - workspaceNumberLimit) + " workspaces" : "";
                String tooltip = availableWorkspaces.stream()
                        .sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getId())))
                        .limit(workspaceNumberLimit)
                        .map(w -> w.getId() + " - " + w.getName() + getUserForWorkspace(w, workspace2ImpersonatedUser))
                        .collect(Collectors.joining(titleNewLine, "Available workspaces are : " + titleNewLine, suffix));
                String icon = String.format("<img style=\"padding-left: 10px;\" src=\"plugin/hp-application-automation-tools-plugin/icons/16x16/info-blue.png\"  title=\"%s\"/>", tooltip);
                msg = msg + icon;
            }

            if (isSuspend != null && isSuspend) {

                msg += "<br/>" + suspendMessage;
            }
            return ConfigurationValidator.wrapWithFormValidation(true, msg);
        } else {
            if (isSuspend != null && isSuspend && !fails.contains(OctaneConnectivityException.UNSUPPORTED_SDK_VERSION_MESSAGE)) {
                fails.add(suspendMessage);
            }
            String errorMsg = "Validation failed : <ul data-aid=\"validation-errors\"><li>" +
                    fails.stream().map(s -> StringEscapeUtils.escapeHtml(s)).collect(Collectors.joining("</li><li>")) +
                    "</li></ul>";
            return ConfigurationValidator.wrapWithFormValidation(false, errorMsg);
        }
    }

    private String getUserForWorkspace(Entity workspace, Map<Long, String> workspace2ImpersonatedUser) {
        if (workspace2ImpersonatedUser == null || workspace2ImpersonatedUser.isEmpty()) {
            return "";
        }
        Long workspaceId = Long.parseLong(workspace.getId());
        if (workspace2ImpersonatedUser.containsKey(workspaceId)) {
            return " (" + workspace2ImpersonatedUser.get(workspaceId) + ")";
        } else {
            return "";
        }
    }

    public OctaneServerSettingsModel[] getServers() {
        return servers;
    }

    public void setServers(OctaneServerSettingsModel... servers) {
        this.servers = servers;
        save();
    }

    public OctaneServerSettingsModel getSettings(String instanceId) {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException("instance ID MUST NOT be null nor empty");
        }

        OctaneServerSettingsModel result = null;
        if (servers != null) {
            for (OctaneServerSettingsModel setting : servers) {
                if (instanceId.equals(setting.getIdentity())) {
                    result = setting;
                    break;
                }
            }
        }
        return result;
    }

    public OctaneServerSettingsModel getSettingsByInternalId(String internalId) {
        if (internalId == null || internalId.isEmpty()) {
            return null;
        }

        OctaneServerSettingsModel result = null;
        if (servers != null) {
            for (OctaneServerSettingsModel setting : servers) {
                if (internalId.equals(setting.getInternalId())) {
                    result = setting;
                    break;
                }
            }
        }
        return result;
    }


    public FormValidation doCheckInstanceId(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("Plugin Instance Id cannot be empty");
        }

        return FormValidation.ok();
    }

    public FormValidation doCheckUiLocation(@QueryParameter String value, @QueryParameter(value = "internalId") String internalId) {
        FormValidation ret = FormValidation.ok();
        //Relevant only for new server configuration (empty internalId)
        if (!StringUtils.isBlank(internalId)) {
            return ret;
        }
        if (StringUtils.isBlank(value)) {
            ret = FormValidation.error("Location must be set");
            return ret;
        }
        OctaneUrlParser octaneUrlParser = null;

        try {
            octaneUrlParser = ConfigurationValidator.parseUiLocation(value);

        } catch (Exception e) {
            ret = FormValidation.error("Failed to parse location.");
        }
        for (OctaneServerSettingsModel serverSettingsModel : servers) {
            if (octaneUrlParser != null && serverSettingsModel.getSharedSpace().equals(octaneUrlParser.getSharedSpace()) &&
                    serverSettingsModel.getLocation().equals(octaneUrlParser.getLocation())) {
                ret = FormValidation.error("This ALM Octane server configuration was already set.");
                return ret;
            }
        }
        return ret;
    }

    private void validateConfiguration(FormValidation result, String formField) throws FormException {
        if (!result.equals(FormValidation.ok())) {
            throw new FormException("Validation of property '" + formField + "' in ALM Octane server Configuration failed: " + result.getMessage(), formField);
        }
    }
}
