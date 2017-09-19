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

import hudson.tasks.BuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.List;

public class NvModel implements Serializable {
    private static final long serialVersionUID = 8642626061740581112L;

    private String serverName;
    private String includeClientIPs;
    private String excludeServerIPs;
    private String envVariable;
    private List<BuildStep> steps;
    private String reportFiles;
    private String thresholdsFile;
    private boolean useProxy;
    private NvServer nvServer;
    private List<NvNetworkProfile> profiles;

    @DataBoundConstructor
    public NvModel(String serverName, String includeClientIPs, String excludeServerIPs, String envVariable, String reportFiles, String thresholdsFile, List<BuildStep> steps) {
        this.serverName = serverName;
        this.includeClientIPs = includeClientIPs;
        this.excludeServerIPs = excludeServerIPs;
        this.envVariable = envVariable;
        this.steps = steps;
        this.reportFiles = reportFiles;
        this.thresholdsFile = thresholdsFile;

        this.useProxy = envVariable != null && !envVariable.isEmpty();
    }

    public String getServerName() {
        return serverName;
    }

    public String getIncludeClientIPs() {
        return includeClientIPs;
    }

    public String getExcludeServerIPs() {
        return excludeServerIPs;
    }

    public String getEnvVariable() {
        return envVariable;
    }

    public List<BuildStep> getSteps() {
        return steps;
    }

    public String getReportFiles() {
        return reportFiles;
    }

    public String getThresholdsFile() {
        return thresholdsFile;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public NvServer getNvServer() {
        return nvServer;
    }

    public void setNvServer(NvServer nvServer) {
        this.nvServer = nvServer;
    }

    public List<NvNetworkProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<NvNetworkProfile> profiles) {
        this.profiles = profiles;
    }
}
