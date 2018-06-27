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

package com.microfocus.application.automation.tools.octane.client;

import com.google.inject.Inject;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationListener;
import com.microfocus.application.automation.tools.octane.configuration.ServerConfiguration;
import hudson.Extension;
import hudson.util.TimeUnit2;

@Extension
public class RetryModel implements ConfigurationListener {
	private static final long[] QUIET_PERIOD_DURATION = {3, 10, 60, 120};

	private long[] QUIET_PERIOD = {
			TimeUnit2.SECONDS.toMillis(QUIET_PERIOD_DURATION[0]),
			TimeUnit2.SECONDS.toMillis(QUIET_PERIOD_DURATION[1]),
			TimeUnit2.SECONDS.toMillis(QUIET_PERIOD_DURATION[2]),
			TimeUnit2.SECONDS.toMillis(QUIET_PERIOD_DURATION[3])
	};

	private long boundary;
	private int periodIndex;

	private TimeProvider timeProvider = new SystemTimeProvider();

	@Inject
	public RetryModel() {
		doSuccess();
	}

	public RetryModel(long... quietPeriods) {
		doSuccess();
		QUIET_PERIOD = quietPeriods;
	}

	public synchronized boolean isQuietPeriod() {
		return timeProvider.getTime() < boundary;
	}

	public synchronized void failure() {
		if (periodIndex < QUIET_PERIOD.length - 1) {
			periodIndex++;
		}
		boundary = timeProvider.getTime() + QUIET_PERIOD[periodIndex];
	}

	public void success() {
		doSuccess();
	}

	private synchronized void doSuccess() {
		periodIndex = -1;
		boundary = 0;
	}

	@Override
	public void onChanged(ServerConfiguration conf, ServerConfiguration oldConf) {
		doSuccess();
	}

	void setTimeProvider(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	private static class SystemTimeProvider implements TimeProvider {

		@Override
		public long getTime() {
			return System.currentTimeMillis();
		}
	}

	interface TimeProvider {
		long getTime();
	}
}
