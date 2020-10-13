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

import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.EntitiesFieldMap;
import com.microfocus.application.automation.tools.commonResultUpload.xmlreader.model.XmlResultEntity;
import hudson.FilePath;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSetReader {

    private XpathReader xr;
    private ValueTranslator translator;
    private EntitiesFieldMap entitiesFieldMap;
    private static final String ROOT = "root";

    public TestSetReader(FilePath filePath, EntitiesFieldMap entitiesFieldMap)
            throws IOException, ParserConfigurationException, InterruptedException, SAXException {
        xr = new XpathReader(filePath);
        translator = new ValueTranslator(xr);
        this.entitiesFieldMap = entitiesFieldMap;
    }

    public TestSetReader(String resultPath, EntitiesFieldMap entitiesFieldMap)
            throws IOException, SAXException, ParserConfigurationException {
        xr = new XpathReader(resultPath);
        translator = new ValueTranslator(xr);
        this.entitiesFieldMap = entitiesFieldMap;
    }

    public List<XmlResultEntity> readTestsets() throws XPathExpressionException {
        List<XmlResultEntity> testsets = readEntities(entitiesFieldMap.getTestset(), xr.getDoc());
        return testsets;
    }

    private List<XmlResultEntity> readEntities(Map<String, String> configMap, Node node)
            throws XPathExpressionException {
        String rootXpath = configMap.get(ROOT);
        rootXpath = rootXpath.substring(2, rootXpath.length());
        NodeList nodes = xr.getNodeListFromNode(rootXpath, node);

        List<XmlResultEntity> entities = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            XmlResultEntity entity = new XmlResultEntity();
            Node currentNode = nodes.item(i);

            Map<String, String> fieldsMap = new HashMap<>();
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                String fieldName = entry.getKey();
                String fieldValue = configMap.get(fieldName);
                if (ROOT.equals(fieldName)) {
                    continue;
                }
                fieldValue = translator.translate(fieldValue, currentNode);
                fieldsMap.put(fieldName, fieldValue);
            }
            entity.setValueMap(fieldsMap);

            Map<String, String> nextConfigMap = entitiesFieldMap.getNextConfigMap(configMap);
            if (nextConfigMap != null) {
                List<XmlResultEntity> subEntities = readEntities(nextConfigMap, currentNode);
                entity.setSubEntities(subEntities);
            }
            entities.add(entity);
        }
        return entities;
    }
}
