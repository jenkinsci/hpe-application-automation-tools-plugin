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

package com.microfocus.application.automation.tools.octane.tests.detection;

/**
 * Class describing metadata of executed tests for test pushing to Octane
 */
public class ResultFields {

    private String framework;
    private String testingTool;
    private String testLevel;
    private String testType;

    public ResultFields() {
    }

    public ResultFields(final String framework, final String testingTool, final String testLevel) {
        this(framework, testingTool, testLevel, null);
    }

    public ResultFields(final String framework, final String testingTool, final String testLevel, final String testType) {
        this.framework = framework;
        this.testingTool = testingTool;
        this.testLevel = testLevel;
        this.testType = testType;
    }

    public String getFramework() {
        return framework;
    }

    public String getTestingTool() {
        return testingTool;
    }

    public String getTestLevel() {
        return testLevel;
    }

    public void setFramework(final String framework) {
        this.framework = framework;
    }

    public void setTestLevel(final String testLevel) {
        this.testLevel = testLevel;
    }

    public void setTestingTool(final String testingTool) {
        this.testingTool = testingTool;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ResultFields that = (ResultFields) o;

        if (framework != null ? !framework.equals(that.framework) : that.framework != null) {
            return false;
        }
        if (testingTool != null ? !testingTool.equals(that.testingTool) : that.testingTool != null) {
            return false;
        }

        if (testType != null ? !testType.equals(that.testType) : that.testType != null) {
            return false;
        }
        return !(testLevel != null ? !testLevel.equals(that.testLevel) : that.testLevel != null);

    }

    @Override
    public int hashCode() {
        int result = framework != null ? framework.hashCode() : 0;
        result = 31 * result + (testingTool != null ? testingTool.hashCode() : 0);
        result = 31 * result + (testLevel != null ? testLevel.hashCode() : 0);
        result = 31 * result + (testType != null ? testType.hashCode() : 0);
        return result;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }
}
