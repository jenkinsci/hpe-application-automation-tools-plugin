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

package com.microfocus.application.automation.tools.octane.actions;

import com.microfocus.application.automation.tools.octane.tests.TestApi;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 12/08/14
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

@Extension
public class BuildActions extends TransientActionFactory<AbstractBuild> {

	static final public class OctaneBuildActions implements RunAction2 {
		private AbstractBuild build;

		OctaneBuildActions(AbstractBuild b) {
			build = b;
		}

		@Override
		public void onAttached(Run<?, ?> run) {
		}

		@Override
		public void onLoad(Run<?, ?> run) {
		}

		@Override
		public String getIconFileName() {
			return null;
		}

		@Override
		public String getDisplayName() {
			return null;
		}

		@Override
		public String getUrlName() {
			return "nga";
		}

		public TestApi getTests() {
			return new TestApi(build);
		}
	}

	@Override
	public Class<AbstractBuild> type() {
		return AbstractBuild.class;
	}

	@Override
	@Nonnull
	public Collection<? extends Action> createFor(@Nonnull AbstractBuild build) {
		ArrayList<Action> actions = new ArrayList<>();
		actions.add(new OctaneBuildActions(build));
		return actions;
	}
}
