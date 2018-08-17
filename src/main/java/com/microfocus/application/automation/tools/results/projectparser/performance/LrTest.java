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

package com.microfocus.application.automation.tools.results.projectparser.performance;

/**
 * Created by kazaky on 08/07/2016.
 */
public interface LrTest {
    public enum SLA_STATUS {
        Failed, Passed, NoData, bad;

        public static SLA_STATUS checkStatus(String status) {
            if ((status.compareTo(Failed.toString()) == 0)) {
                return Failed;
            } else if ((status.compareTo(Passed.toString()) == 0)) {
                return Passed;
            } else if ((status.compareTo(NoData.toString()) == 0)) {
                return NoData;
            }
            return bad;
        }
    }

    enum SLA_GOAL {
        AverageThroughput, TotalThroughput, AverageHitsPerSecond, TotalHits,
        ErrorsPerSecond, PercentileTRT, AverageTRT, Bad;

        public static SLA_GOAL checkGoal(String status) {
            if ((status.compareTo(AverageThroughput.toString()) == 0)) {
                return AverageThroughput;
            } else if ((status.compareTo(TotalThroughput.toString()) == 0)) {
                return TotalThroughput;
            } else if ((status.compareTo(AverageHitsPerSecond.toString()) == 0)) {
                return AverageHitsPerSecond;
            } else if ((status.compareTo(TotalHits.toString()) == 0)) {
                return TotalHits;
            } else if ((status.compareTo(ErrorsPerSecond.toString()) == 0)) {
                return ErrorsPerSecond;
            } else if ((status.compareTo(AverageTRT.toString()) == 0)) {
                return AverageTRT;
            } else if ((status.compareTo(PercentileTRT.toString()) == 0)) {
                return PercentileTRT;
            }
            return Bad;
        }

    }
}
