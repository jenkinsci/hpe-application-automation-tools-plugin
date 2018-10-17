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

package com.microfocus.application.automation.tools.settings;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.model.OctaneServerSettingsModel;
import com.microfocus.application.automation.tools.octane.CIJenkinsServicesImpl;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationListener;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationParser;
import com.microfocus.application.automation.tools.octane.configuration.MqmProject;
import com.microfocus.application.automation.tools.octane.configuration.ServerConfiguration;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.acegisecurity.context.SecurityContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Octane configuration settings
 */

public class OctaneServerSettingsBuilder extends Builder {
	private static final Logger logger = LogManager.getLogger(OctaneServerSettingsBuilder.class);

	@Override
	public OctaneDescriptorImpl getDescriptor() {
		return (OctaneDescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link OctaneServerSettingsBuilder}. Used as a singleton. The class is marked as
	 * public so that it can be accessed from views.
	 * <p>
	 * <p>
	 * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the
	 * actual HTML fragment for the configuration screen.
	 */

	@Extension
	public static final class OctaneDescriptorImpl extends BuildStepDescriptor<Builder> {

		@CopyOnWrite
		private OctaneServerSettingsModel[] servers;

		@Override
		protected XmlFile getConfigFile() {
			XmlFile xmlFile = super.getConfigFile();
			//Between 5.1 to 5.2 - migration hp->hpe was done.
			//Old configuration file 'com.hp.application.automation.tools.settings.OctaneServerSettingsBuilder.xml'
			//is replaced by new one 'com.hpe.application.automation.tools.settings.OctaneServerSettingsBuilder.xml'.
			//As well, inside the configuration, there were replaces of hp->hpe
			//if xmlFile is not exist, we will check if configuration file name exist in format of 5.1 version
			//if so, we will copy old configuration to new one with replacements of hp->hpe
			if (!xmlFile.exists()) {
				//try to get from old path
				File oldConfigurationFile = new File(xmlFile.getFile().getPath().replace("hpe", "hp"));
				if (oldConfigurationFile.exists()) {
					try {
						String configuration = FileUtils.readFileToString(oldConfigurationFile);
						String newConfiguration = StringUtils.replace(configuration, ".hp.", ".hpe.");
						FileUtils.writeStringToFile(xmlFile.getFile(), newConfiguration);
						xmlFile = super.getConfigFile();
					} catch (IOException e) {
						logger.error("failed to copy ALM Octane Plugin configuration 5.1 to new 5.2 format : " + e.getMessage());
					}
				}
			}

			return xmlFile;
		}

		public OctaneDescriptorImpl() {
			load();

			OctaneServerSettingsModel model = getModel();
			if (StringUtils.isEmpty(model.getIdentity())) {
				model.setIdentity(UUID.randomUUID().toString());
				model.setIdentityFrom(new Date().getTime());
				save();
			}

			try {
				OctaneSDK.init(new CIJenkinsServicesImpl());
			} catch (IllegalStateException ise) {
				logger.warn("Octane SDK already initialized");
			}
		}

		private static ServerConfiguration convertToServerConfiguration(OctaneServerSettingsModel model) {
			return new ServerConfiguration(
					model.getLocation(),
					model.getSharedSpace(),
					model.getUsername(),
					model.getPassword(),
					model.getImpersonatedUser(),
					model.isSuspend());
		}

		@Override
		public boolean isApplicable(
				@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
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

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

			JSONObject jsonObject = (JSONObject) formData.get("mqm");
			List<OctaneServerSettingsModel> list = req.bindJSONToList(OctaneServerSettingsModel.class, jsonObject);
			OctaneServerSettingsModel newModel = list.get(0);

			if (jsonObject.containsKey("showIdentity")) {
				JSONObject showIdentityJo = (JSONObject) jsonObject.get("showIdentity");
				String identity = showIdentityJo.getString("identity");
				validateConfiguration(doCheckInstanceId(identity), "Plugin instance id");

				OctaneServerSettingsModel oldModel = getModel();
				if (!oldModel.getIdentity().equals(identity)) {
					newModel.setIdentity(identity);
				}
			}
			setModel(newModel);
			return super.configure(req, formData);
		}

		public void setModel(OctaneServerSettingsModel newModel) {
			//infer uiLocation
			MqmProject mqmProject;
			try {
				mqmProject = ConfigurationParser.parseUiLocation(newModel.getUiLocation());
				newModel.setSharedSpace(mqmProject.getSharedSpace());
				newModel.setLocation(mqmProject.getLocation());
			} catch (FormValidation fv) {
				logger.warn("tested configuration failed on Octane URL parse: " + fv.getMessage(), fv);
			}

			OctaneServerSettingsModel oldModel = getModel();

			//set identity in new model
			boolean identityChanged = StringUtils.isNotEmpty(newModel.getIdentity());
			if (!identityChanged) { //set identity from old model
				newModel.setIdentity(oldModel.getIdentity());
				newModel.setIdentityFrom(oldModel.getIdentityFrom());
			}

			servers = (new OctaneServerSettingsModel[]{newModel});
			save();

			ServerConfiguration oldConfiguration = convertToServerConfiguration(oldModel);
			ServerConfiguration newConfiguration = convertToServerConfiguration(newModel);
			if (!oldConfiguration.equals(newConfiguration)) {
				fireOnChanged(newConfiguration, oldConfiguration);
			}
		}

		private void fireOnChanged(ServerConfiguration configuration, ServerConfiguration oldConfiguration) {
			OctaneSDK.getInstance().getConfigurationService().notifyChange();
			ExtensionList<ConfigurationListener> listeners = ExtensionList.lookup(ConfigurationListener.class);
			for (ConfigurationListener listener : listeners) {
				try {
					listener.onChanged(configuration, oldConfiguration);
				} catch (ThreadDeath t) {
					throw t;
				} catch (Throwable t) {
					logger.warn(t);
				}
			}
		}

		@SuppressWarnings("unused")
		public FormValidation doTestConnection(@QueryParameter("uiLocation") String uiLocation,
		                                       @QueryParameter("username") String username,
		                                       @QueryParameter("password") String password,
		                                       @QueryParameter("impersonatedUser") String impersonatedUser) {
			MqmProject mqmProject;
			try {
				mqmProject = ConfigurationParser.parseUiLocation(uiLocation);
			} catch (FormValidation fv) {
				logger.warn("tested configuration failed on Octane URL parse: " + fv.getMessage(), fv);
				return fv;
			}

			//  if parse is good, check authentication/authorization
			ConfigurationParser parser = Jenkins.getInstance().getExtensionList(ConfigurationParser.class).iterator().next();
			FormValidation validation = parser.checkConfiguration(mqmProject.getLocation(), mqmProject.getSharedSpace(), username, Secret.fromString(password));

			//  if still good, check Jenkins user permissions
			try {
				SecurityContext preserveContext = impersonate(impersonatedUser);
				if (!Jenkins.getInstance().hasPermission(Item.READ)) {
					logger.warn("tested configuration failed on insufficient Jenkins' user permissions");
					validation = FormValidation.errorWithMarkup(ConfigurationParser.markup("red", Messages.JenkinsUserPermissionsFailure()));
				}
				depersonate(preserveContext);
			} catch (FormValidation fv) {
				logger.warn("tested configuration failed on impersonating Jenkins' user, most likely non existent user provided", fv);
				return fv;
			}

			return validation;
		}

		public OctaneServerSettingsModel[] getServers() {
			if (servers == null) {
				servers = new OctaneServerSettingsModel[]{new OctaneServerSettingsModel()};
			}
			return servers;
		}

		public OctaneServerSettingsModel getModel() {
			return getServers()[0];
		}

		public ServerConfiguration getServerConfiguration() {
			return convertToServerConfiguration(getModel());
		}

		private SecurityContext impersonate(String user) throws FormValidation {
			SecurityContext originalContext = null;
			if (user != null && !user.equalsIgnoreCase("")) {
				User jenkinsUser = User.get(user, false, Collections.emptyMap());
				if (jenkinsUser != null) {
					originalContext = ACL.impersonate(jenkinsUser.impersonate());
				} else {
					throw FormValidation.errorWithMarkup(ConfigurationParser.markup("red", Messages.JenkinsUserPermissionsFailure()));
				}
			}
			return originalContext;
		}

		private void depersonate(SecurityContext originalContext) {
			if (originalContext != null) {
				ACL.impersonate(originalContext.getAuthentication());
			}
		}

		public FormValidation doCheckInstanceId(@QueryParameter String value) {
			if (StringUtils.isBlank(value)) {
				return FormValidation.error("Plugin Instance Id cannot be empty");
			}

			return FormValidation.ok();
		}

		private void validateConfiguration(FormValidation result, String formField) throws FormException {
			if (!result.equals(FormValidation.ok())) {
				throw new FormException("Validation of property in ALM Octane Server Configuration failed: " + result.getMessage(), formField);
			}
		}
	}
}
