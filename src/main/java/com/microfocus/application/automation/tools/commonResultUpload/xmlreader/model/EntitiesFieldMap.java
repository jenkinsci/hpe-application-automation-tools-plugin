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
 * © Copyright 2012-2019 Micro Focus or one of its affiliates..
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

package com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model;

import java.util.Map;

public class EntitiesFieldMap {

    private Map<String, String> testset;
    private Map<String, String> test;
    private Map<String, String> run;

    private EntitiesFieldMap() {

    }

    public Map<String, String> getTestset() {
        return testset;
    }
    public void setTestset(Map<String, String> testset) {
        this.testset = testset;
    }

    public Map<String, String> getTest() {
        return test;
    }
    public void setTest(Map<String, String> test) {
        this.test = test;
    }

    public Map<String, String> getRun() {
        return run;
    }
    public void setRun(Map<String, String> run) {
        this.run = run;
    }

    public Map<String, String> getNextConfigMap(Map<String, String> map) {
        if (map.hashCode() == testset.hashCode()) {
            return test;
        } else if (map.hashCode() == test.hashCode()) {
            return run;
        } else {
            return null;
        }
    }
}
