/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright (c) 2018 Micro Focus Company, L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
