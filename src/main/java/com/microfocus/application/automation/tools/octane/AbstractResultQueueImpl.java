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
	public synchronized void add(String projectName, int buildNumber) {
		queue.add(new QueueItem(projectName, buildNumber));
	}

	@Override
	public synchronized void add(String projectName, String type, int buildNumber) {
		queue.add(new QueueItem(projectName, type, buildNumber));
	}

	public int size() {
		return queue.size();
	}

	@Override
	public synchronized void add(String projectName, int buildNumber, String workspace) {
		queue.add(new QueueItem(projectName, buildNumber, workspace));
	}

	@Override
	public synchronized void clear() {
		while (queue.size() > 0) {
			queue.remove();
		}
		currentItem = null;
	}

	private static class JsonConverter implements FileObjectQueue.Converter<QueueItem> {

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
			return queueItem;
		}

		private static JSONObject jsonFromObject(QueueItem item) {
			JSONObject json = new JSONObject();
			json.put("project", item.projectName);
			json.put("build", item.buildNumber);
			json.put("count", item.failCount);
			json.put("workspace", item.workspace);
			json.put("type", item.type);
			return json;
		}
	}
}
