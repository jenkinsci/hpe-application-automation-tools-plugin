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

package com.microfocus.application.automation.tools.results;

import hudson.model.DirectoryBrowserSupport;
import hudson.model.ModelObject;
import hudson.model.Run;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Models the HTML summary reports
 */
public class SummaryReport implements ModelObject {

    private String name = "";
    private String color = "";
    private String duration = "";
    private String pass = "";
    private String fail = "";
    private Run<?,?> build = null;
    private DirectoryBrowserSupport _directoryBrowserSupport = null;

    /**
     * Instantiates a new Summary report.
     *
     * @param build                   the build
     * @param name                    the name
     * @param directoryBrowserSupport the directory browser support
     */
    public SummaryReport(Run<?,?> build, String name, DirectoryBrowserSupport directoryBrowserSupport) {
        this.build = build;
        this.name = name;
        _directoryBrowserSupport = directoryBrowserSupport;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    /**
     * Gets build.
     *
     * @return the build
     */
    public Run<?,?> getBuild() {
        return build;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Do dynamic.
     *
     * @param req the req
     * @param rsp the rsp
     * @throws IOException      the io exception
     * @throws ServletException the servlet exception
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {

        if (_directoryBrowserSupport != null)
            _directoryBrowserSupport.generateResponse(req, rsp, this);
    }

    /**
     * Gets color.
     *
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets color.
     *
     * @param value the value
     */
    public void setColor(String value) {
        color = value;
    }

    /**
     * Gets duration.
     *
     * @return the duration
     */
    public String getDuration() {
        return duration;
    }

    /**
     * Sets duration.
     *
     * @param value the value
     */
    public void setDuration(String value) {
        duration = value;
    }

    /**
     * Gets pass.
     *
     * @return the pass
     */
    public String getPass() {
        return pass;
    }

    /**
     * Sets pass.
     *
     * @param value the value
     */
    public void setPass(String value) {
        pass = value;
    }

    /**
     * Gets fail.
     *
     * @return the fail
     */
    public String getFail() {
        return fail;
    }

    /**
     * Sets fail.
     *
     * @param value the value
     */
    public void setFail(String value) {
        fail = value;
    }

}
