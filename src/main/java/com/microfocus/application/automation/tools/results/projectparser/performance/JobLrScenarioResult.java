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

package com.microfocus.application.automation.tools.results.projectparser.performance;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;


/**
 * Holds information on the SLA's of one scenario (per job / run / build)
 */
public class JobLrScenarioResult extends LrScenario {
    public static final int DEFAULT_CONNECTION_MAX = -1;
    public static final int DEFAULT_SCENARIO_DURATION = -1;
    public ArrayList<GoalResult> scenarioSlaResults;
    public Map<String, Integer> vUserSum;
    public TreeMap<String, Integer> transactionSum;
    public TreeMap<String, TreeMap<String, Integer>> transactionData;
    private int connectionMax;
    private long scenarioDuration;

    public JobLrScenarioResult(String scenarioName) {
        super.setScenrioName(scenarioName);
        connectionMax = DEFAULT_CONNECTION_MAX;
        scenarioDuration = DEFAULT_SCENARIO_DURATION;
        vUserSum = new TreeMap<String, Integer>();
        transactionSum = new TreeMap<String, Integer>();
        transactionData = new TreeMap<>();
        scenarioSlaResults = new ArrayList<GoalResult>(0);
    }

    public long getScenarioDuration() {
        return scenarioDuration;
    }

    public void setScenarioDuration(long scenarioDuration) {
        this.scenarioDuration = scenarioDuration;
    }

    public int getConnectionMax() {
        return connectionMax;
    }

    public void setConnectionMax(int connectionMax) {
        this.connectionMax = connectionMax;
    }
}
