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

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class NvResultsMerger {

    public static void mergeResults(AbstractBuild<?, ?> build, String reportFilesPattern, NvJUnitResult result) throws IOException, InterruptedException {
        FilePath workspace = build.getWorkspace();
        FilePath[] files = workspace.list(reportFilesPattern);
        //TODO- itay: filter files according build time
        for (FilePath file : files) {
            file.act(new MergeFileCallable(result));
        }
    }

    public static class MergeFileCallable implements FilePath.FileCallable<Void> {
        private static final long serialVersionUID = 1498088030227367799L;

        private NvJUnitResult result;

        public MergeFileCallable(NvJUnitResult result) {
            this.result = result;
        }

        @Override
        public Void invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document;
            try {
                document = saxBuilder.build(file);
            } catch (JDOMException e) {
                e.printStackTrace();
                return null;
            }
            Element rootElement = document.getRootElement();

            for (Element suiteElement : getSuiteElements(rootElement)) {
                updateSuiteElement(suiteElement);
                replaceTestCaseElements(suiteElement);
            }

            document.setContent(rootElement);

            FileWriter writer = null;
            try {
                writer = new FileWriter(file);
                XMLOutputter xmlOutputter = new XMLOutputter();
                xmlOutputter.setFormat(Format.getPrettyFormat());
                xmlOutputter.output(document, writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();

                if(null != writer) {
                    writer.close();
                }
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }

            return null;
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            // do nothing
        }

        private Collection<Element> getSuiteElements(Element rootElement) {
            Collection<Element> result = new ArrayList<>();

            if (isSuiteElement(rootElement)) {
                result.add(rootElement);
            } else {
                for (Element element : rootElement.getChildren()) {
                    if (isSuiteElement(element)) {
                        result.add(element);
                    }
                }
            }

            return result;
        }

        private void replaceTestCaseElements(Element suiteElement) {
            suiteElement.removeChildren(JUnitXmlConstants.TEST_ELEMENT);

            List<Element> newTcElements = new ArrayList<>();
            for (NvClassResult classResult : result.getNvTestSuiteResult(suiteElement.getAttribute(JUnitXmlConstants.SUITE_ELEMENT_ATTR_NAME).getValue()).getResults()) {
                for (NvProfileResult profileResult : classResult.getResults()) {
                    for (NvTestCaseResult testCaseResult : profileResult.getResults()) {
                        newTcElements.add(XmlTestCaseElementCreator.create(classResult.getName(), profileResult.getName(), testCaseResult));
                    }
                }
            }

            Collections.sort(newTcElements, new Comparator<Element>() {
                @Override
                public int compare(Element o1, Element o2) {
                    if (o1 == o2) {
                        return 0;
                    }
                    if (o1 == null) {
                        return -1;
                    }
                    if (o2 == null) {
                        return 1;
                    }
                    int classComp = o1.getAttribute(JUnitXmlConstants.TEST_ELEMENT_ATTR_CLASS).getValue().compareToIgnoreCase(o2.getAttribute(JUnitXmlConstants.TEST_ELEMENT_ATTR_CLASS).getValue());
                    if (classComp == 0) {
                        return o1.getAttribute(JUnitXmlConstants.TEST_ELEMENT_ATTR_NAME).getValue().compareToIgnoreCase(o2.getAttribute(JUnitXmlConstants.TEST_ELEMENT_ATTR_NAME).getValue());
                    }
                    return classComp;
                }
            });

            suiteElement.addContent(newTcElements);
        }

        private boolean isSuiteElement(Element element) {
            return element.getName().equals(JUnitXmlConstants.SUITE_ELEMENT);
        }

        private void updateSuiteElement(Element suiteElement) {
            String suiteName = suiteElement.getAttribute(JUnitXmlConstants.SUITE_ELEMENT_ATTR_NAME).getValue();
            NvTestSuiteResult nvSuiteResult = result.getNvTestSuiteResult(suiteName);
            if (null != nvSuiteResult) {
                suiteElement.setAttribute(JUnitXmlConstants.SUITE_ELEMENT_ATTR_TIME, String.valueOf(nvSuiteResult.getDuration()));
                suiteElement.setAttribute(JUnitXmlConstants.SUITE_ELEMENT_ATTR_SKIPS, String.valueOf(nvSuiteResult.getSkipCount()));
                suiteElement.setAttribute(JUnitXmlConstants.SUITE_ELEMENT_ATTR_FAILS, String.valueOf(nvSuiteResult.getFailCount()));
                suiteElement.setAttribute(JUnitXmlConstants.SUITE_ELEMENT_ATTR_ERRORS, String.valueOf(nvSuiteResult.getErrorCount()));
                suiteElement.setAttribute(JUnitXmlConstants.SUITE_ELEMENT_ATTR_TESTS, String.valueOf(
                        nvSuiteResult.getSkipCount() + nvSuiteResult.getFailCount() + nvSuiteResult.getErrorCount() + nvSuiteResult.getPassCount()));
            }
        }
    }
}
