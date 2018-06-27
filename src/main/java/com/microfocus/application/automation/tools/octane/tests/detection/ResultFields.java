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
