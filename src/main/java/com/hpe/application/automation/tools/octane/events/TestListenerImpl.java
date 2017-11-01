/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hpe.application.automation.tools.octane.events;

import com.google.inject.Inject;
import com.hpe.application.automation.tools.octane.tests.TestListener;
import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;

/**
 * Listener on job complete event
 */
@Extension
public class TestListenerImpl extends RunListener<Run> {
	@Inject
	private TestListener testListener;

	@Override
	public void onCompleted(Run r, @Nonnull TaskListener listener) {
		testListener.processBuild(r, listener);
	}
}
