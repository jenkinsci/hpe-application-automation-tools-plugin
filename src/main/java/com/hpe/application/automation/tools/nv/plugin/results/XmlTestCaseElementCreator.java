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

import org.jdom2.Element;

public class XmlTestCaseElementCreator {

    public static Element create(String className, String profileName, NvTestCaseResult testCaseResult) {
        Element element = new Element(JUnitXmlConstants.TEST_ELEMENT);
        element.setAttribute(JUnitXmlConstants.TEST_ELEMENT_ATTR_NAME, testCaseResult.getName() + "_" + profileName.replace(" ", ""));
        element.setAttribute(JUnitXmlConstants.TEST_ELEMENT_ATTR_CLASS, className);
        element.setAttribute(JUnitXmlConstants.TEST_ELEMENT_ATTR_TIME, String.valueOf(testCaseResult.getDuration()));
        if(testCaseResult.isSkipped()) {
            element.addContent(createSkipped());
        } else if(testCaseResult.isFail()) {
            element.addContent(createFailure(testCaseResult));
        } else if(testCaseResult.isError()) {
            element.addContent(createError(testCaseResult));
        }

        return element;
    }

    private static Element createSkipped() {
        return new Element(JUnitXmlConstants.TEST_SKIP_ELEMENT);
    }

    private static Element createFailure(NvTestCaseResult testCaseResult) {
        Element element = new Element(JUnitXmlConstants.TEST_FAILURE_ELEMENT);
        element.setAttribute(JUnitXmlConstants.TEST_FAILURE_ELEMENT_ATTR_MESSAGE, testCaseResult.getErrorMessage());
        element.setAttribute(JUnitXmlConstants.TEST_FAILURE_ELEMENT_ATTR_TYPE, extractErrorType(testCaseResult.getErrorStackTrace()));
        element.setText(testCaseResult.getErrorStackTrace());

        return element;
    }

    private static Element createError(NvTestCaseResult testCaseResult) {
        Element element = new Element(JUnitXmlConstants.TEST_ERROR_ELEMENT);
        element.setAttribute(JUnitXmlConstants.TEST_ERROR_ELEMENT_ATTR_MESSAGE, testCaseResult.getErrorMessage());
        element.setAttribute(JUnitXmlConstants.TEST_ERROR_ELEMENT_ATTR_TYPE, extractErrorType(testCaseResult.getErrorStackTrace()));
        element.setText(testCaseResult.getErrorStackTrace());

        return element;
    }

    private static String extractErrorType(String errorStack) {
        if (null != errorStack && !errorStack.isEmpty()) {
            return errorStack.substring(0, errorStack.indexOf(":"));
        } else {
            return "";
        }
    }
}
