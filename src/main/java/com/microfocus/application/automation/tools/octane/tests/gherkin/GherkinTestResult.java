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

package com.microfocus.application.automation.tools.octane.tests.gherkin;

import com.microfocus.application.automation.tools.octane.tests.junit.TestResultStatus;
import com.microfocus.application.automation.tools.octane.tests.testResult.TestResult;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by franksha on 20/03/2016.
 */
public class GherkinTestResult implements TestResult {
    private Map<String, String> attributes;
    private Element contentElement;

    public GherkinTestResult(String name, Element xmlElement, long duration, TestResultStatus status) {
        this.attributes = new HashMap<>();
        this.attributes.put("name", name);
        this.attributes.put("duration", String.valueOf(duration));
        this.attributes.put("status", status.toPrettyName());
        this.contentElement = xmlElement;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Element getXmlElement() {
        return contentElement;
    }

    @Override
    public void writeXmlElement(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("gherkin_test_run");
        if (attributes != null) {
            for (String attrName : attributes.keySet()) {
                writer.writeAttribute(attrName, attributes.get(attrName));
            }
        }
        writeXmlElement(writer, contentElement);
        writer.writeEndElement();
    }

    private void writeXmlElement(XMLStreamWriter writer, Element rootElement) throws XMLStreamException {
        if (rootElement != null) {
            writer.writeStartElement(rootElement.getTagName());
            for (int a = 0; a < rootElement.getAttributes().getLength(); a++) {
                String attrName = rootElement.getAttributes().item(a).getNodeName();
                writer.writeAttribute(attrName, rootElement.getAttribute(attrName));
            }
            NodeList childNodes = rootElement.getChildNodes();
            for (int c = 0; c < childNodes.getLength(); c++) {
                Node child = childNodes.item(c);
                if (child instanceof Element) {
                    writeXmlElement(writer, (Element) child);
                } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    writer.writeCharacters(child.getNodeValue());
                }
            }
            writer.writeEndElement();
        }
    }
}
