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
package com.microfocus.application.automation.tools.lr.model;

import com.microfocus.application.automation.tools.lr.Messages;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.BooleanUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import javax.annotation.Nonnull;
import java.util.Properties;

public class SummaryDataLogModel extends AbstractDescribableImpl<SummaryDataLogModel> {
    private boolean logVusersStates;
    private boolean logErrorCount;
    private boolean logTransactionStatistics;
    private String pollingInterval;

    @DataBoundConstructor
    public SummaryDataLogModel(boolean logVusersStates, boolean logErrorCount, boolean logTransactionStatistics, String pollingInterval) {
        this.logVusersStates = logVusersStates;
        this.logErrorCount = logErrorCount;
        this.logTransactionStatistics = logTransactionStatistics;
        this.pollingInterval = pollingInterval;
    }

    public boolean getLogVusersStates() {
        return logVusersStates;
    }

    public boolean getLogErrorCount() {
        return logErrorCount;
    }

    public boolean getLogTransactionStatistics() {
        return logTransactionStatistics;
    }

    public String getPollingInterval() { return pollingInterval; }

    public void addToProps(Properties props)
    {
        if (StringUtils.isEmpty(pollingInterval))
        {
            pollingInterval = "30";
        }

        props.put("SummaryDataLog",
                BooleanUtils.toInteger(logVusersStates) + ";"
                + BooleanUtils.toInteger(logErrorCount) + ";"
                + BooleanUtils.toInteger(logTransactionStatistics) + ";"
                + Integer.parseInt(pollingInterval)
        );
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SummaryDataLogModel> {
        @Nonnull
        public String getDisplayName() { return Messages.SummaryDataLogModel(); }

        public FormValidation doCheckPollingInterval(@QueryParameter String value) {
            if (!StringUtils.isNumeric(value)) {
                return FormValidation.error("Polling Interval must be a number");
            }

            return FormValidation.ok();
        }
    }
}
