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

package com.microfocus.application.automation.tools.results.projectparser.performance;

/**
 * The type Lr scenario.
 */
public abstract class LrScenario extends LrJobResults {
    private String _scenrioName;

    /**
     * Instantiates a new Lr scenario.
     */
    public LrScenario() {
        this("");
    }

    /**
     * Instantiates a new Lr scenario.
     *
     * @param _scenrioName the scenrio name
     */
    public LrScenario(String _scenrioName) {
        this._scenrioName = _scenrioName;
    }

    /**
     * Gets scenario name.
     *
     * @return the scenario name
     */
    public String getScenarioName() {
        return _scenrioName;
    }

    /**
     * Sets scenrio name.
     *
     * @param scenrioName the scenrio name
     */
    void setScenrioName(String scenrioName) {
        _scenrioName = scenrioName;
    }
}
