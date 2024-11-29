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
package com.microfocus.application.automation.tools.octane;

import com.squareup.tape.FileObjectQueue;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Created by benmeior on 11/21/2016
 *
 * Base implementation of ResultQueue: backed up by FileObjectQueue, persisted
 */

public abstract class AbstractResultQueueImpl implements ResultQueue {

	private static final int RETRIES = 3;
	private final int MAX_RETRIES;

	private FileObjectQueue<QueueItem> queue;

	private QueueItem currentItem;

	public AbstractResultQueueImpl() {
		this.MAX_RETRIES = RETRIES;
	}

	public AbstractResultQueueImpl(int maxRetries) {
		this.MAX_RETRIES = maxRetries;
	}

	protected void init(File queueFile) throws IOException {
		queue = new FileObjectQueue<>(queueFile, new JsonConverter());
	}

	@Override
	public synchronized QueueItem peekFirst() {
		if (currentItem == null) {
			currentItem = queue.peek();
		}
		return currentItem;
	}

	@Override
	public synchronized boolean failed() {
		if (currentItem != null) {
			boolean retry;
			if (++currentItem.failCount <= MAX_RETRIES) {
				queue.add(currentItem);
				retry = true;
			} else {
				retry = false;
			}

			remove();

			return retry;
		} else {
			throw new IllegalStateException("no outstanding item");
		}
	}

	@Override
	public synchronized void remove() {
		if (currentItem != null) {
			queue.remove();
			currentItem = null;
		} else {
			throw new IllegalStateException("no outstanding item");
		}
	}

	@Override
	public synchronized void add(QueueItem item) {
		queue.add(item);
	}

	@Override
	public synchronized void add(String projectName, int buildNumber) {
		queue.add(new QueueItem(projectName, buildNumber));
	}

	@Override
	public synchronized void add(String projectName, String type, int buildNumber) {
		queue.add(new QueueItem(projectName, type, buildNumber));
	}

	@Override
	public synchronized void add(String projectName, int buildNumber, String workspace) {
		queue.add(new QueueItem(projectName, buildNumber, workspace));
	}

	@Override
	public synchronized void add(String instanceId, String projectName, int buildNumber, String workspace) {
		QueueItem item = new QueueItem(projectName, buildNumber, workspace);
		item.setInstanceId(instanceId);
		queue.add(item);
	}

	public int size() {
		return queue.size();
	}

	@Override
	public synchronized void clear() {
		while (queue.size() > 0) {
			queue.remove();
		}
		currentItem = null;
	}

	@Override
	public void close() {
		if (queue != null) {
			queue.close();
		}
	}

	private static class JsonConverter implements FileObjectQueue.Converter<QueueItem> {

		public static final String INSTANCE_ID = "instanceId";

		@Override
		public QueueItem from(byte[] bytes) throws IOException {
			JSONObject json = (JSONObject) JSONSerializer.toJSON(IOUtils.toString(new ByteArrayInputStream(bytes)));
			return objectFromJson(json);
		}

		@Override
		public void toStream(QueueItem item, OutputStream bytes) throws IOException {
			JSONObject json = jsonFromObject(item);
			OutputStreamWriter writer = new OutputStreamWriter(bytes);
			writer.append(json.toString());
			writer.close();
		}

		private static QueueItem objectFromJson(JSONObject json) {
			QueueItem queueItem = json.containsKey("workspace") ?
					new QueueItem(
							json.getString("project"),
							json.getInt("build"),
							json.getInt("count"),
							json.getString("workspace")) :
					new QueueItem(
							json.getString("project"),
							json.getInt("build"),
							json.getInt("count"));
			if (json.containsKey("type")) {
				queueItem.setType(json.getString("type"));
			}
			if (json.containsKey("sendAfter")) {
				queueItem.setSendAfter(json.getLong("sendAfter"));
			}
			if (json.containsKey(INSTANCE_ID)) {
				queueItem.setInstanceId(json.getString(INSTANCE_ID));
			}
			return queueItem;
		}

		private static JSONObject jsonFromObject(QueueItem item) {
			JSONObject json = new JSONObject();
			json.put("project", item.projectName);
			json.put("build", item.buildNumber);
			json.put("count", item.failCount);
			json.put("workspace", item.workspace);
			json.put("type", item.type);
			json.put("sendAfter", item.sendAfter);
            json.put(INSTANCE_ID, item.instanceId);
			return json;
		}
	}
}
