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
