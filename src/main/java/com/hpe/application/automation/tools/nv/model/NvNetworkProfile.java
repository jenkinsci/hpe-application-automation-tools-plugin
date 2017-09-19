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

package com.hpe.application.automation.tools.nv.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import com.hpe.application.automation.tools.nv.common.NvValidatorUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;

@JsonIgnoreProperties({"serialVersionUID", "dto"})
public class NvNetworkProfile extends AbstractDescribableImpl<NvNetworkProfile> implements Serializable {
    private static final long serialVersionUID = 5600302656112810974L;

    private String profileName;
    private String latency = "0";
    private String packet = "0";
    private String bandwidthIn = BandwidthEnum.UNRESTRICTED.getDisplayText();
    private String bandwidthOut = BandwidthEnum.UNRESTRICTED.getDisplayText();

    private boolean custom = true;
    private NvProfileDTO dto;

    @JsonCreator
    @DataBoundConstructor
    public NvNetworkProfile(@JsonProperty("profileName") String profileName) {
        this.profileName = profileName;
    }

    public String getProfileName() {
        return profileName;
    }

    private void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getLatency() {
        return latency;
    }

    @DataBoundSetter
    public void setLatency(String latency) {
        this.latency = latency;
    }

    public String getPacket() {
        return packet;
    }

    @DataBoundSetter
    public void setPacket(String packet) {
        this.packet = packet;
    }

    public String getBandwidthIn() {
        return bandwidthIn;
    }

    @DataBoundSetter
    public void setBandwidthIn(String bandwidthIn) {
        this.bandwidthIn = bandwidthIn;
    }

    public String getBandwidthOut() {
        return bandwidthOut;
    }

    @DataBoundSetter
    public void setBandwidthOut(String bandwidthOut) {
        this.bandwidthOut = bandwidthOut;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    @Override
    public String toString() {
        return "Network Profile{" +
                "profileName='" + profileName + '\'' +
                ", latency='" + latency + '\'' +
                ", packet='" + packet + '\'' +
                ", bandwidthIn='" + bandwidthIn + '\'' +
                ", bandwidthOut='" + bandwidthOut + '\'' +
                '}' + "\n";
    }

    public NvProfileDTO toDTO() {
        if (null == dto) {
            dto = new NvProfileDTO(profileName);
            dto.setLatency(Double.parseDouble(latency));
            dto.setPacket(Double.parseDouble(packet));
            dto.setBandwidthIn(BandwidthEnum.valueOf(bandwidthIn).getValue());
            dto.setBandwidthOut(BandwidthEnum.valueOf(bandwidthOut).getValue());
        }

        return dto;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<NvNetworkProfile> {
        public static final int MAX_LATENCY_VALUE = 8000;
        public static final int MAX_PACKET_VALUE = 100;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        public FormValidation doCheckProfileName(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a profile name");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckLatency(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set latency");
            }
            if (!NvValidatorUtils.validateFloatingPoint(value, MAX_LATENCY_VALUE)) {
                return FormValidation.error("Latency must be a positive number between 0 and " + MAX_LATENCY_VALUE);
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckPacket(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set packet loss");
            }
            if (!NvValidatorUtils.validateFloatingPoint(value, MAX_PACKET_VALUE)) {
                return FormValidation.error("Packet loss must be a positive number between 0 and " + MAX_PACKET_VALUE);
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillBandwidthInItems() {
            return getBandwidthItems();
        }

        public ListBoxModel doFillBandwidthOutItems() {
            return getBandwidthItems();
        }

        private ListBoxModel getBandwidthItems() {
            ListBoxModel items = new ListBoxModel();
            for (BandwidthEnum bandwidthEnum : BandwidthEnum.values()) {
                items.add(bandwidthEnum.getDisplayText(), bandwidthEnum.name());
            }

            return items;
        }

        @Override
        public String getDisplayName() {
            return "Network Profile";
        }
    }
}
