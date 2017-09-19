/*
    (c) Copyright [2016] Hewlett Packard Enterprise Development LP

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
    rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
    persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
    Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
    OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.hpe.application.automation.tools.nv.plugin;

import com.hpe.application.automation.tools.nv.common.NvNetworkProfileRegistry;
import com.hpe.application.automation.tools.nv.common.NvValidatorUtils;
import com.hpe.application.automation.tools.nv.model.NvModel;
import com.hpe.application.automation.tools.nv.model.NvNetworkProfile;
import com.hpe.application.automation.tools.nv.model.NvServer;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.*;

import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class NvEmulationBuilder extends Builder {

    private final NvModel nvModel;
    private Set<NetworkProfileSelector> profileNames = new HashSet<>();

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public NvEmulationBuilder(String serverName, String includeClientIPs, String excludeServerIPs, String reportFiles, String thresholdsFile, UseProxyCheckbox useProxyCheckbox, List<BuildStep> steps) {
        String envVariable = useProxyCheckbox == null ? null : useProxyCheckbox.getEnvVariable();

        nvModel = new NvModel(serverName, includeClientIPs, excludeServerIPs, envVariable, reportFiles, thresholdsFile, steps);
        nvModel.setNvServer(getNvServer(serverName));
    }

    public NvModel getNvModel() {
        return nvModel;
    }

    public String getServerName() {
        return nvModel.getServerName();
    }

    public NetworkProfileSelector[] getProfileNames() {
        if (null == profileNames) {
            profileNames = new HashSet<>();
        }
        return profileNames.toArray(new NetworkProfileSelector[profileNames.size()]);
    }

    @DataBoundSetter
    public void setProfileNames(final NetworkProfileSelector[] profileNames) {
        if (profileNames != null) {
            this.profileNames.addAll(Arrays.asList(profileNames));

            List<String> profileNamesList = new ArrayList<>();
            for (NetworkProfileSelector profileName : profileNames) {
                profileNamesList.add(profileName.getProfileName());
            }

            nvModel.setProfiles(NvNetworkProfileRegistry.getInstance().getNetworkProfiles(profileNamesList));
        }
    }

    private NvServer getNvServer(String serverName) {
        return getDescriptor().getNvServer(serverName);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return new NvEmulationInvoker(nvModel, build, launcher, listener).invoke();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private List<NvNetworkProfile> customProfiles = new ArrayList<>();
        private List<NvServer> nvServers = new ArrayList<>();

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
            registerCustomProfiles();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            customProfiles = new ArrayList<>(req.bindJSONToList(NvNetworkProfile.class, formData.get("customProfiles")));
            unregisterCustomProfiles();
            registerCustomProfiles();

            nvServers = new ArrayList<>(req.bindJSONToList(NvServer.class, formData.get("nvServers")));

            save();
            return super.configure(req, formData);
        }

        private void registerCustomProfiles() {
            NvNetworkProfileRegistry.getInstance().register(customProfiles);
        }

        private void unregisterCustomProfiles() {
            NvNetworkProfileRegistry.getInstance().unregisterCustom();
        }

        public NvNetworkProfile[] getCustomProfiles() {
            return customProfiles.toArray(new NvNetworkProfile[customProfiles.size()]);
        }

        public NvServer[] getNvServers() {
            return nvServers.toArray(new NvServer[nvServers.size()]);
        }

        public NvServer getNvServer(String serverName) {
            NvServer result = null;
            for (NvServer nvServer : nvServers) {
                if (serverName.equals(nvServer.getServerName())) {
                    result = nvServer;
                    break;
                }
            }

            return result;
        }

        public ListBoxModel doFillServerNameItems() {
            ListBoxModel items = new ListBoxModel();
            for (NvServer nvServer : nvServers) {
                items.add(nvServer.getServerName(), nvServer.getServerName());
            }

            return items;
        }

        public FormValidation doCheckIncludeClientIPs(@QueryParameter String value) throws IOException, ServletException {
            return validateIPs(value);
        }

        public FormValidation doCheckExcludeServerIPs(@QueryParameter String value) throws IOException, ServletException {
            return validateIPs(value);
        }

        private FormValidation validateIPs(String value) {
            value = value.replace(" ", "");
            String[] ips = value.split(";");
            for (String ip : ips) {
                if (!ip.isEmpty()) {
                    if (ip.contains("-")) {
                        String[] ranges = ip.split("-");
                        if (!NvValidatorUtils.isValidHostIp(ranges[0]) || !NvValidatorUtils.isValidHostIp(ranges[1])) {
                            return FormValidation.error("IP range must contain valid IPv4 addresses");
                        }
                    } else {
                        if (!NvValidatorUtils.isValidHostIp(ip)) {
                            return FormValidation.error("IP must be a valid IPv4 address");
                        }
                    }
                }
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckUseProxyCheckbox(@QueryParameter String value, @QueryParameter String serverName) throws IOException, ServletException {
            NvServer nvServer = getNvServer(serverName);
            if(null != nvServer) {
                boolean isProxyChecked = Boolean.parseBoolean(value);
                if(isProxyChecked) {
                    if(nvServer.getProxyPort().isEmpty()) {
                        return FormValidation.error("The proxy port for the selected NV Test Manager was not configured");
                    }
                }
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckEnvVariable(@QueryParameter String value) throws IOException, ServletException {
            if (value.trim().length() == 0) {
                return FormValidation.error("Please set an environment variable name");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckReportFiles(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException, ServletException {
            if (value.trim().length() == 0) {
                return FormValidation.error("Please set pattern for test report XMLs");
            }

            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        public FormValidation doCheckThresholdsFile(@QueryParameter String value) throws IOException, ServletException {
            if (value.trim().length() == 0) {
                return FormValidation.error("Please set file location");
            }

            if (!NvValidatorUtils.validateFile(value)) {
                return FormValidation.error("File can not be read. Check that the file exists and can be accessed");
            }
            try {
                if (null == NvValidatorUtils.readThresholdsFile(value)) {
                    return FormValidation.error("File must contain valid threshold records. Please check the help tooltip");
                }
            } catch (IOException e) {
                return FormValidation.error("An error occurred while trying to read the specified file");
            }

            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "HPE Network Virtualization";
        }

        public List<? extends Descriptor<? extends BuildStep>> getSteps(AbstractProject<?, ?> project) {
            final List<BuildStepDescriptor<? extends Builder>> builders = new ArrayList<BuildStepDescriptor<? extends Builder>>();
            if (null == project) {
                return builders;
            }
            for (Descriptor<Builder> descriptor : Builder.all()) {
                if (descriptor instanceof NvEmulationBuilder.DescriptorImpl) {
                    continue;
                }
                BuildStepDescriptor<? extends Builder> buildStepDescriptor = (BuildStepDescriptor) descriptor;
                if (buildStepDescriptor.isApplicable(project.getClass()) && hasDbc(buildStepDescriptor.clazz)) {
                    builders.add(buildStepDescriptor);
                }
            }
            return builders;
        }

        private boolean hasDbc(final Class<?> clazz) {
            for (Constructor<?> constructor : clazz.getConstructors()) {
                if (constructor.isAnnotationPresent(DataBoundConstructor.class)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class UseProxyCheckbox {
        private String envVariable;

        @DataBoundConstructor
        public UseProxyCheckbox(String envVariable) {
            this.envVariable = envVariable;
        }

        public String getEnvVariable() {
            return envVariable;
        }
    }
}
