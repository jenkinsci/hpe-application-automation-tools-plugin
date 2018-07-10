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

package com.hpe.application.automation.tools.octane.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

/**
 * This class Converts UFT's Parameters info stored in Resource.mtr file into JSON string
 */
public class UFTParameterFactory {


    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(UFTParameterFactory.class.getName());


    public static String convertResourceMtrAsJSON(InputStream resourceMtrInputStream) throws IOException {

        //String QTPFileParameterFileName = "resource.mtr";
        //InputStream is = paths.get(0).getParent().child("Action0").child(QTPFileParameterFileName).read();

        String xmlData = UFTTestUtil.decodeXmlContent(resourceMtrInputStream);

        try {
            SAXBuilder saxBuilder = new SAXBuilder(XMLReaders.NONVALIDATING, null, null);
            Document document = saxBuilder.build(new StringReader(xmlData));
            Element rootElement = document.getRootElement();
            List<Element> rootChildrenElements = rootElement.getChildren();
            ArrayList<UFTParameter> uftParameters = new ArrayList<>();
            for (int temp = 0; temp < rootChildrenElements.size(); temp++) {
                Element tag = rootChildrenElements.get(temp);
                if ("ArgumentsCollection".equalsIgnoreCase(tag.getName())) {
                    List<Element> children = tag.getChildren();
                    for (int i = 0; i < children.size(); i++) {
                        UFTParameter uftParameter = new UFTParameter();
                        Element element = children.get(i);
                        List<Element> elements = element.getChildren();

                        for (int j = 0; j < elements.size(); j++) {

                            Element element1 = elements.get(j);
                            switch (element1.getName()) {
                                case "ArgName":
                                    uftParameter.setArgName(element1.getValue());
                                    break;
                                case "ArgDirection":
                                    uftParameter.setArgDirection(Integer.parseInt(element1.getValue()));
                                    break;
                                case "ArgDefaultValue":
                                    uftParameter.setArgDefaultValue(element1.getValue());
                                    break;
                                case "ArgType":
                                    uftParameter.setArgType(element1.getValue());
                                    break;
                                case "ArgIsExternal":
                                    uftParameter.setArgIsExternal(Integer.parseInt(element1.getValue()));
                                    break;
                                default:
                                    logger.warning(String.format("Element name %s didn't match any case", element1.getName()));
                                    break;
                            }
                        }
                        uftParameters.add(uftParameter);
                    }
                    ObjectMapper mapper = new ObjectMapper();
                    String result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(uftParameters);
                    return result;
                }
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return null;
    }

    public static String convertApiTestXmlsAsJSON(File inputParameters, File outputParameters) throws IOException {
        //String testInputParametersFileName = "TestInputParameters.xml";
        //String testOutputParametersFileName = "TestOutputParameters.xml";
        //URI inputParamsUri = paths.get(0).getParent().child(testInputParametersFileName).toURI();
        //URI outputParamsUri = paths.get(0).getParent().child(testOutputParametersFileName).toURI();

        //File inputParamsFile = new File(inputParamsUri);
        //File outputParamsFile = new File(outputParamsUri);


        List<UFTParameter> params = new ArrayList<>();
        params.addAll(convertApiTestXmlToArguments(inputParameters, true));
        params.addAll(convertApiTestXmlToArguments(outputParameters, false));
        ObjectMapper mapper = new ObjectMapper();
        String result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(params);
        return result;
    }

    public static Collection<UFTParameter> convertApiTestXmlToArguments(File parametersFile, boolean isInputParameters) throws IOException {

        /*<TestParameters>
            <Schema>
                <xsd:schema xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:types="http://hp.vtd.schemas/types/v1.0" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                    <xs:import schemaLocation="../../../dat/schemas/Types.xsd" namespace="http://hp.vtd.schemas/types/v1.0" />
                    <xs:element types:displayName="Parameters" name="Arguments">
                        <xs:complexType>
                            <xs:sequence>
                                <xs:element types:masked="false" name="StartParam1" type="xs:long">
                                    <xs:annotation>
                                        <xs:documentation />
                                    </xs:annotation>
                                </xs:element>
                                <xs:element types:masked="false" name="endParam1" type="xs:string">
                                    <xs:annotation>
                                        <xs:documentation />
                                    </xs:annotation>
                                </xs:element>
                            </xs:sequence>
                        </xs:complexType>
                    </xs:element>
                </xsd:schema>
            </Schema>
            <Values>
                <Arguments>
                    <StartParam1>1</StartParam1>
                    <endParam1>f</endParam1>
                </Arguments>
            </Values>
        </TestParameters>*/

        try {
            SAXBuilder saxBuilder = new SAXBuilder(XMLReaders.NONVALIDATING, null, null);
            Document document = saxBuilder.build(parametersFile);
            Element rootElement = document.getRootElement();

            Map<String, UFTParameter> uftParametersMap = new HashMap<>();
            List<Element> argElements = getHierarchyChildElement(rootElement, "Schema", "schema", "element", "complexType", "sequence").getChildren();
            for (Element argElement : argElements) {
                String name = argElement.getAttributeValue("name");
                String type = argElement.getAttributeValue("type").replace("xs:", "");
                int direction = isInputParameters ? 0 : 1;

                UFTParameter parameter = new UFTParameter();
                parameter.setArgName(name);
                parameter.setArgType(type);
                parameter.setArgDirection(direction);
                uftParametersMap.put(parameter.getArgName(), parameter);
            }

            //getArg default values
            List<Element> argDefValuesElements = getHierarchyChildElement(rootElement, "Values", "Arguments").getChildren();
            for (Element argElement : argDefValuesElements) {
                UFTParameter parameter = uftParametersMap.get(argElement.getName());
                if (parameter != null) {
                    parameter.setArgDefaultValue(argElement.getValue());
                }
            }

            return uftParametersMap.values();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return Collections.emptySet();
    }

    private static Element getHierarchyChildElement(Element root, String... childPath) {
        Element parent;
        Element found = root;
        for (int i = 0; i < childPath.length; i++) {
            parent = found;
            found = null;
            String elementName = childPath[i];

            for (Element child : parent.getChildren()) {
                if (child.getName().equals(elementName)) {
                    found = child;
                    break;
                }
            }
        }

        return found;
    }
}



