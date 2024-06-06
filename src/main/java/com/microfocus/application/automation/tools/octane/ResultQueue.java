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

package com.microfocus.application.automation.tools.octane;

import java.io.Serializable;

@SuppressWarnings("squid:S2039")
public interface ResultQueue {

	ResultQueue.QueueItem peekFirst();

	boolean failed();

	void remove();

	void add(QueueItem item);

	void add(String projectName, int buildNumber);

	void add(String projectName, String type, int buildNumber);

	void add(String projectName, int buildNumber, String workspace);

	void add(String instanceId, String projectName, int buildNumber, String workspace);

	void clear();

	void close();

	class QueueItem implements Serializable {
		private static final long serialVersionUID = 1;
		String instanceId;
		public String type;
		String projectName;
		int buildNumber;
		String workspace;
		int failCount;
		long sendAfter;

		public void setInstanceId(String instanceId) {
			this.instanceId = instanceId;
		}

		public void setType(String type) {
			this.type = type;
		}

		public QueueItem(String projectName, int buildNumber) {
			this(projectName, buildNumber, 0);
		}

		public QueueItem(String projectName, String type, int buildNumber) {
			this(projectName, buildNumber, 0);
			this.type = type;
		}

		public QueueItem(String projectName, int buildNumber, String workspace) {
			this(projectName, buildNumber, 0);
			this.workspace = workspace;
		}

		QueueItem(String projectName, int buildNumber, int failCount) {
			this.projectName = projectName;
			this.buildNumber = buildNumber;
			this.failCount = failCount;
		}

		QueueItem(String projectName, int buildNumber, int failCount, String workspace) {
			this.projectName = projectName;
			this.buildNumber = buildNumber;
			this.failCount = failCount;
			this.workspace = workspace;
		}

		public String getInstanceId() {
			return instanceId;
		}

		public String getType() {
			return type;
		}

		public int incrementFailCount() {
			return this.failCount++;
		}

		public int getFailCount() {
			return failCount;
		}

		public String getProjectName() {
			return projectName;
		}

		public int getBuildNumber() {
			return buildNumber;
		}

		public String getWorkspace() {
			return workspace;
		}

		public long getSendAfter() {
			return sendAfter;
		}

		public void setSendAfter(long sendAfter) {
			this.sendAfter = sendAfter;
		}
	}
}
