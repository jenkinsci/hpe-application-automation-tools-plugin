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
