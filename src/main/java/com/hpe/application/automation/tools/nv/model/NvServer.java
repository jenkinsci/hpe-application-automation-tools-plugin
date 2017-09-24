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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import com.hpe.application.automation.tools.nv.common.NvValidatorUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;

@JsonIgnoreProperties({"serialVersionUID"})
public class NvServer extends AbstractDescribableImpl<NvServer> implements Serializable {
    private static final long serialVersionUID = -5822820278864676678L;

    private String serverName;
    private String serverIp;
    private String nvPort;
    private String proxyPort;
    private String username;
    private String password;

    @DataBoundConstructor
    public NvServer(String serverName, String serverIp, String nvPort, String proxyPort, String username, String password) {
        this.serverName = serverName;
        this.serverIp = serverIp;
        this.nvPort = nvPort;
        this.proxyPort = proxyPort;
        this.username = username;
        this.password = password;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerIp() {
        return serverIp;
    }

    public String getNvPort() {
        return nvPort;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "NvServer{" +
                "serverName='" + serverName + '\'' +
                ", serverIp='" + serverIp + '\'' +
                ", nvPort='" + nvPort + '\'' +
                ", proxyPort='" + proxyPort + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<NvServer> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        public FormValidation doCheckServerName(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a server name");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckServerIp(@QueryParameter String value) throws IOException, ServletException {
            value = value.trim();
            if (value.length() == 0) {
                return FormValidation.error("Please set an IPv4 address");
            } else if (!NvValidatorUtils.isValidHostIp(value)) {
                return FormValidation.error("Value must be a valid IPv4 address");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckNvPort(@QueryParameter String value) throws IOException, ServletException {
            return validatePort(value, true);
        }

        public FormValidation doCheckProxyPort(@QueryParameter String value) throws IOException, ServletException {
            return validatePort(value, false);
        }

        private FormValidation validatePort(String value, boolean isMandatory) {
            value = value.trim();
            if (isMandatory && value.length() == 0) {
                return FormValidation.error("Please set a port");
            }
            try {
                int port = 0;
                if (value.length() > 0) {
                    port = Integer.parseInt(value);
                    if (port <= 0) {
                        return FormValidation.error("Port must be a positive integer");
                    }
                }
            } catch (NumberFormatException e) {
                return FormValidation.error("Port must be a positive integer");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckUsername(@QueryParameter String value) throws IOException, ServletException {
            if (value.trim().length() == 0) {
                return FormValidation.error("Please enter a username");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter String value) throws IOException, ServletException {
            if (value.trim().length() == 0) {
                return FormValidation.error("Please enter a password");
            }

            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "NV Test Manager";
        }
    }
}
