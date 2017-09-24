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

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import com.hpe.application.automation.tools.nv.common.NvNetworkProfileRegistry;
import com.hpe.application.automation.tools.nv.model.BandwidthEnum;
import com.hpe.application.automation.tools.nv.model.NvNetworkProfile;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.IOException;

public class NetworkProfileSelector extends AbstractDescribableImpl<NetworkProfileSelector> {
    private final String profileName;

    @DataBoundConstructor
    public NetworkProfileSelector(final String profileName) {
        super();

        this.profileName = profileName;
    }

    public String getProfileName() {
        return profileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NetworkProfileSelector)) {
            return false;
        }

        NetworkProfileSelector that = (NetworkProfileSelector) o;

        return profileName.equals(that.profileName);
    }

    @Override
    public int hashCode() {
        return profileName.hashCode();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<NetworkProfileSelector> {
        private final String TOOLTIP_FORMAT = "Latency (ms): %s\n" +
                "Packet Loss (%%): %s\n" +
                "Bandwidth In (Kbps): %s\n" +
                "Bandwidth Out (Kbps): %s";

        private String defaultProfileName;
        private String selectedProfileName;

        public DescriptorImpl() {
            load();
            // set default for the first usage
            defaultProfileName = NvNetworkProfileRegistry.getInstance().getNetworkProfilesAsListModel().get(0).name;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        /**
         * Returns the available parsers. These values will be shown in the list
         * box of the config.jelly view part.
         * @return the model of the list box
         */
        public ListBoxModel doFillProfileNameItems() {
            // get model each time since model may be changed with custom profiles
            return NvNetworkProfileRegistry.getInstance().getNetworkProfilesAsListModel();
        }

        public FormValidation doCheckProfileName(@QueryParameter String value) throws IOException, ServletException {
            if(!NvNetworkProfileRegistry.getInstance().exists(value)) {
                return FormValidation.error("Profile does not exist");
            }

            return FormValidation.ok();
        }

        public String getDefaultProfileName() {
            return defaultProfileName;
        }

        public String getSelectedProfileName() {
            return selectedProfileName;
        }

        @JavaScriptMethod
        public String getTooltip(String profileName) {
            NvNetworkProfile networkProfile = NvNetworkProfileRegistry.getInstance().getNetworkProfile(profileName);
            return String.format(TOOLTIP_FORMAT, networkProfile.getLatency(), networkProfile.getPacket(), BandwidthEnum.valueOf(networkProfile.getBandwidthIn()).getValue(), BandwidthEnum.valueOf(networkProfile.getBandwidthOut()).getValue());
        }

        @Override
        public String getDisplayName() {
            return StringUtils.EMPTY;
        }
    }
}