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

package com.microfocus.application.automation.tools.octane.tests;

import com.microfocus.application.automation.tools.octane.ResultQueue;
import hudson.model.AbstractBuild;
import org.junit.Assert;

import java.util.Collection;
import java.util.LinkedList;

@SuppressWarnings("squid:S2925")
public class TestQueue implements ResultQueue {

	private LinkedList<QueueItem> queue = new LinkedList<>();
	private int discard;

	@Override
	public synchronized QueueItem peekFirst() {
		++ticks;
		if (!queue.isEmpty()) {
			return queue.getFirst();
		} else {
			return null;
		}
	}

	@Override
	public synchronized boolean failed() {
		QueueItem item = queue.removeFirst();
		if (item.incrementFailCount() < 1) {
			queue.add(item);
			return true;
		} else {
			++discard;
			return false;
		}
	}

	@Override
	public synchronized void remove() {
		queue.removeFirst();
	}

	@Override
	public synchronized void add(String projectName, int buildNumber) {
		queue.add(new QueueItem(projectName, buildNumber));
	}

	/**
	 * add task to queue, type is not relevant to to test queue
	 * @param projectName
	 * @param type
	 * @param buildNumber
	 */
	@Override
	public void add(String projectName, String type, int buildNumber) {
		queue.add(new QueueItem(projectName, buildNumber));
	}

	@Override
	public void add(String projectName, int buildNumber, String workspace) {
		queue.add(new QueueItem(projectName, buildNumber, workspace));
	}

	public synchronized void add(Collection<? extends AbstractBuild> builds) {
		for (AbstractBuild build : builds) {
			queue.add(new QueueItem(build.getProject().getName(), build.getNumber()));
		}
	}

	@Override
	public synchronized void clear() {
		while (!queue.isEmpty()) {
			queue.remove();
		}
	}

	public synchronized int size() {
		return queue.size();
	}

	public synchronized int getDiscards() {
		return discard;
	}

	//  test usage only; [YG] - TODO: remove this code from here ASAP
	private long ticks;
	public void waitForTicks(int n) throws InterruptedException {
		long current = ticks;
		long target = current + n;
		for (int i = 0; i < 2000; i++) {
			current = ticks;
			if (current >= target) {
				return;
			}
			Thread.sleep(10);
		}
		Assert.fail("Timed out: ticks: expected=" + target + "; actual=" + current);
	}
}
