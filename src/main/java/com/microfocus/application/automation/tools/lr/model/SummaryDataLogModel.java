/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
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
