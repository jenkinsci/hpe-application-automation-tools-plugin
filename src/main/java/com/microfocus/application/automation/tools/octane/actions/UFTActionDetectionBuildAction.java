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

import com.hp.octane.integrations.uft.items.*;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.executor.UFTTestDetectionService;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class responsible to show report of  {@link UFTTestDetectionService} for MBT
 */
public class UFTActionDetectionBuildAction implements Action {

    private AbstractBuild<?, ?> build;

    private UftTestDiscoveryResult results;

    private List<UftTestAction> actions;

    private Map<String, List<UftTestParameter>> actionToParametersMap;

    private List<UftTestParameter> parameters;

    public UFTActionDetectionBuildAction(final AbstractBuild<?, ?> build, UftTestDiscoveryResult results) {
        this.build = build;

        setResults(results);
    }

    private List<? extends SupportsOctaneStatus> getItemsWithStatus(List<? extends SupportsOctaneStatus> entities, OctaneStatus status) {
        return entities.stream()
                .filter(item -> status.equals(item.getOctaneStatus()))
                .collect(Collectors.toList());
    }

    private List<UftTestAction> flattenActions(UftTestDiscoveryResult results) {
        return results.getAllTests().stream()
                .map(AutomatedTest::getActions)
                .flatMap(Collection::stream)
                .filter(action -> !action.getOctaneStatus().equals(OctaneStatus.NONE))
                .collect(Collectors.toList());
    }

    private List<UftTestParameter> flattenParameters(List<UftTestAction> actions) {
        return actions.stream()
                .map(UftTestAction::getParameters)
                .flatMap(Collection::stream)
                .filter(parameter -> !parameter.getOctaneStatus().equals(OctaneStatus.NONE))
                .collect(Collectors.toList());
    }

    private Map<String, List<UftTestParameter>> setActionToParametersMap() {
        return actions.stream()
                .filter(action -> CollectionUtils.isNotEmpty(action.getParameters()))
                .collect(Collectors.toMap(UftTestAction::getRepositoryPath, UftTestAction::getParameters));
    }

    @Override
    public String getIconFileName() {
        return "notepad.png";
    }

    @Override
    public String getDisplayName() {
        return Messages.UFTActionDetectionBuildActionConfigurationLabel();
    }

    @Override
    public String getUrlName() {
        return "uft_report";
    }

    @SuppressWarnings("squid:S1452")
    public final Run<?, ?> getBuild() {
        return build;
    }

    public UftTestDiscoveryResult getResults() {
        return results;
    }

    public void setResults(UftTestDiscoveryResult results) {
        this.results = results;
        init(results);
    }

    public List<UftTestAction> getActions() {
        return actions;
    }

    public void setActions(List<UftTestAction> actions) {
        this.actions = actions;
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTActionDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasNewActions() {
        return getNewActions().size() > 0;
    }

    public List<UftTestAction> getNewActions() {
        return (List<UftTestAction>) getItemsWithStatus(actions, OctaneStatus.NEW);
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTActionDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasDeletedActions() {
        return getDeletedActions().size() > 0;
    }

    public List<UftTestAction> getDeletedActions() {
        return (List<UftTestAction>) getItemsWithStatus(actions, OctaneStatus.DELETED);
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTActionDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasUpdatedActions() {
        return getUpdatedActions().size() > 0;
    }

    public List<UftTestAction> getUpdatedActions() {
        return (List<UftTestAction>) getItemsWithStatus(actions, OctaneStatus.MODIFIED);
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTActionDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasNewParameters() {
        return getNewParameters().size() > 0;
    }

    public List<UftTestParameter> getNewParameters() {
        return (List<UftTestParameter>) getItemsWithStatus(parameters, OctaneStatus.NEW);
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTActionDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasDeletedParameters() {
        return getDeletedParameters().size() > 0;
    }

    public List<UftTestParameter> getDeletedParameters() {
        return (List<UftTestParameter>) getItemsWithStatus(parameters, OctaneStatus.DELETED);
    }

    /**
     * used by ~\src\main\resources\com\hp\application\automation\tools\octane\actions\UFTActionDetectionBuildAction\index.jelly
     *
     * @return
     */
    public boolean getHasUpdatedParameters() {
        return getUpdatedParameters().size() > 0;
    }

    public List<UftTestParameter> getUpdatedParameters() {
        return (List<UftTestParameter>) getItemsWithStatus(parameters, OctaneStatus.MODIFIED);
    }

    public boolean getHasQuotedPaths() {
        return results.isHasQuotedPaths();
    }

    public Map<String, List<UftTestParameter>> getActionToParametersMap() {
        return actionToParametersMap;
    }

    private void init(UftTestDiscoveryResult results) {
        boolean isEmptyResults = results == null;
        if (isEmptyResults) {
            this.results = new UftTestDiscoveryResult();
            this.actions = Collections.emptyList();
            this.actionToParametersMap = Collections.emptyMap();
            this.parameters = Collections.emptyList();
        } else {
            this.actions = flattenActions(results);
            this.actionToParametersMap = setActionToParametersMap();
            this.parameters = flattenParameters(actions);
        }
    }

}
