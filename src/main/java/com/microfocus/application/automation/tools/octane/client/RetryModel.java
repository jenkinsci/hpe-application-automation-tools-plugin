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
