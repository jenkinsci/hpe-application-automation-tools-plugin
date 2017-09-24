/*
    (c) Copyright [2016] Hewlett Packard Enterprise Development LP

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
    rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
    persons to whom the Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
    Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
    WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
    OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.hpe.application.automation.tools.nv.plugin.results;

public class JUnitXmlConstants {
    public static final String SUITE_ELEMENT = "testsuite";
    public static final String SUITE_ELEMENT_ATTR_NAME = "name";
    public static final String SUITE_ELEMENT_ATTR_TIME = "time";
    public static final String SUITE_ELEMENT_ATTR_TESTS = "tests";
    public static final String SUITE_ELEMENT_ATTR_ERRORS = "errors";
    public static final String SUITE_ELEMENT_ATTR_SKIPS = "skipped";
    public static final String SUITE_ELEMENT_ATTR_FAILS = "failures";

    public static final String TEST_ELEMENT = "testcase";
    public static final String TEST_ELEMENT_ATTR_NAME = "name";
    public static final String TEST_ELEMENT_ATTR_CLASS = "classname";
    public static final String TEST_ELEMENT_ATTR_TIME = "time";

    public static final String TEST_ERROR_ELEMENT = "error";
    public static final String TEST_ERROR_ELEMENT_ATTR_MESSAGE = "message";
    public static final String TEST_ERROR_ELEMENT_ATTR_TYPE = "type";

    public static final String TEST_FAILURE_ELEMENT = "failure";
    public static final String TEST_FAILURE_ELEMENT_ATTR_MESSAGE = "message";
    public static final String TEST_FAILURE_ELEMENT_ATTR_TYPE = "type";

    public static final String TEST_SKIP_ELEMENT = "skipped";
}
