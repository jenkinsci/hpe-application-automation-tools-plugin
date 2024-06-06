
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

// instance.getTotalHitsGraphData(function(t){
//     var r = t.responseObject();
//     var ctx = document.getElementById('TotalHits').getContext("2d");
//     new Chart(ctx).Line(r);
// });
//
//
// instance.getAvgHitsPerSecGraphData(function(t){
//     var data = t.responseObject();
//
//     var option = {};
//     var ctx = document.getElementById('AvgHitsPerSecGraphData').getContext("2d");
//     new Chart(ctx).Line(data);
// });
//
// instance.getTotalThroughputGraphData(function(t){
//     var data = t.responseObject();
//     var ctx = document.getElementById('TotalThroughput').getContext("2d");
//     new Chart(ctx).Line(data);
//
// });
//
// instance.getAvgThroughtputResultsGraphData(function(t){
//     var data = t.responseObject();
//     var ctx = document.getElementById('AvgThroughput').getContext("2d");
//     new Chart(ctx).Line(data);
//
// });
//
//
// instance.getAvgTransactionResultsGraphData(function(t){
//     var data = t.responseObject();
//     var ctx = document.getElementById('AvgTRT').getContext("2d");
//     var a = new Chart(ctx).Line(data);
//
//     document.getElementById('AvgTRT_Legend').innerHTML = a.generateLegend();
//
// });
//
// instance.getErrorPerSecResultsGraphData(function(t){
//     var data = t.responseObject();
//         var ctx = document.getElementById('ErrorPerSec').getContext("2d");
//         new Chart(ctx).Line(data);
// });
//
// instance.getPercentelieTransactionResultsGraphData(function(t){
//     var data = t.responseObject();
//         var ctx = document.getElementById('PercentileTransactionWholeRun').getContext("2d");
//         var a = new Chart(ctx).Line(data);
//         document.getElementById('PercentileTransactionWholeRun_Legend').innerHTML = a.generateLegend();
// });
