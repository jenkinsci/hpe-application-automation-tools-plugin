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

package com.microfocus.application.automation.tools.commonResultUpload.xmlreader;

import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;

public class ValueTranslator {

    private XpathReader xpathReader;

    public ValueTranslator(XpathReader xpathReader) {
        this.xpathReader = xpathReader;
    }

    public String translate(String fieldValue, Node node) throws XPathExpressionException {
        String[] arr = fieldValue.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (String i : arr) {
            sb.append(translateUnit(i, node));
        }
        return sb.toString();
    }

    private String translateUnit(String fieldValue, Node node) throws XPathExpressionException {
        if ("v:".equals(fieldValue.substring(0, 2))) {
            fieldValue = fieldValue.substring(2, fieldValue.length());
        } else if ("x:".equals(fieldValue.substring(0, 2))) {
            fieldValue = fieldValue.substring(2, fieldValue.length());
            fieldValue = xpathReader.getValueFromNode(fieldValue, node);
        }
        return fieldValue;
    }
}
