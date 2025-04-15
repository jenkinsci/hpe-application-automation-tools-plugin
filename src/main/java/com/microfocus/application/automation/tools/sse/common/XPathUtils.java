/*
 *  Certain versions of software accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now
 *  offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical
 *  in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the
 *  property of their respective owners.
 *  OpenText is a trademark of Open Text.
 *  __________________________________________________________________
 *  MIT License
 *
 *  Copyright 2012-2025 Open Text.
 *
 *  The only warranties for products and services of Open Text and
 *  its affiliates and licensors ("Open Text") are as may be set forth
 *  in the express warranty statements accompanying such products and services.
 *  Nothing herein should be construed as constituting an additional warranty.
 *  Open Text shall not be liable for technical or editorial errors or
 *  omissions contained herein. The information contained herein is subject
 *  to change without notice.
 *
 *  Except as specifically indicated otherwise, this document contains
 *  confidential information and a valid license is required for possession,
 *  use or copying. If this work is provided to the U.S. Government,
 *  consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *  Computer Software Documentation, and Technical Data for Commercial Items are
 *  licensed to the U.S. Government under vendor's standard commercial license.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ___________________________________________________________________
 */
package com.microfocus.application.automation.tools.sse.common;

import java.io.StringReader;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/***
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */

public class XPathUtils {

    public static Map<String, String> getEntityFieldsMap(String xml) {
        Document document = getDocument(xml);
        NodeList entities = document.getElementsByTagName("Field");
        Map<String, String> entityFieldsMap = new HashMap<String, String>();
        for (int i = 0; i < entities.getLength(); i++) {
            Element element = ((Element) entities.item(i));
            entityFieldsMap.put(element.getAttribute("Label"), element.getAttribute("Name"));
        }
        return entityFieldsMap;
    }

    public static Map<String, String> getEntitySubtypesMap(String xml) {
        Document document = getDocument(xml);
        NodeList entities = document.getElementsByTagName("type");
        Map<String, String> customizationMap = new HashMap<String, String>();
        for (int i = 0; i < entities.getLength(); i++) {
            Element element = ((Element) entities.item(i));
            customizationMap.put(element.getAttribute("name"), element.getAttribute("id"));
        }
        return customizationMap;
    }
    
    public static List<Map<String, String>> toEntities(String xml) {
        
        Document document = getDocument(xml);
        
        List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
        NodeList entities = document.getElementsByTagName("Entity");
        for (int i = 0; i < entities.getLength(); i++) {
            Map<String, String> currEntity = new HashMap<String, String>();
            NodeList fields = ((Element) entities.item(i)).getElementsByTagName("Field");
            for (int j = 0; j < fields.getLength(); j++) {
                Node item = fields.item(j);
                currEntity.put(item.getAttributes().item(0).getNodeValue(), getFieldValue(item));
            }
            ret.add(currEntity);
        }
        
        return ret;
    }
    
    public static String getAttributeValue(String xml, String attrName) {
        
        NodeList nodes = getChildNodes(xml, "Entity/Fields/Field");
        String ret = StringUtils.EMPTY_STRING;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node currNode = nodes.item(i);
            String attr;
            try {
                attr = getNecessaryAttribute(currNode, "Name");
            } catch (Throwable cause) {
                throw new SSEException(cause);
            }
            if (attr.equals(attrName)) {
                ret = getFieldValue(currNode);
                break;
            }
        }
        
        return ret;
    }
    
    private static String getFieldValue(Node node) {
        
        String ret = null;
        Node child = node.getFirstChild();
        if (child != null) {
            Node child2 = child.getFirstChild();
            if (child2 != null) {
                ret = child2.getNodeValue();
            }
        }
        
        return ret;
    }
    
    private static NodeList getChildNodes(String xml, String xpath) {
        
        NodeList ret = null;
        try {
            Document document = getDocument(xml);
            XPathFactory factory = XPathFactory.newInstance();
            XPathExpression expression = factory.newXPath().compile(xpath);
            ret = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
        } catch (Throwable cause) {
            throw new SSEException(cause);
        }
        
        return ret;
    }
    
    private static String getNecessaryAttribute(Node node, String attributeName) {
        
        if (!node.hasAttributes()) {
            return null;
        }
        Node attr = node.getAttributes().getNamedItem(attributeName);
        if (attr == null) {
            throw new SSEException(String.format(
                    "Error parsing XML, missing mandatory attribute '%s'",
                    attributeName));
        }
        String ret = attr.getNodeValue();
        if (StringUtils.isNullOrEmpty(ret)) {
            throw new SSEException(String.format(
                    "Error parsing XML, mandatory attribute '%s' cannot be empty", //$NON-NLS-1$
                    attributeName));
        }
        
        return ret;
    }
    
    public static Document getDocument(String xml) {
        
        Document ret = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = dbf.newDocumentBuilder();

            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(xml));
            ret = builder.parse(inputSource);
        } catch (Throwable cause) {
            throw new SSEException(cause);
        }
        
        return ret;
    }

    public static boolean hasResults(String xml) {
        boolean ok = false;

        try {
            Document doc = getDocument(xml);
            if (doc == null) {
                return ok;
            }

            Node root = doc.getDocumentElement();
            if (root == null) {
                return ok;
            }

            if ((root.hasAttributes() && Integer.parseInt(root.getAttributes().getNamedItem("TotalResults").getNodeValue()) > 0)
                || (doc.hasChildNodes() && doc.getElementsByTagName("Entity").getLength() > 0)) {
                ok = true;
            }
        } catch (SSEException | NumberFormatException cause) {
            throw new SSEException(cause);
        }

        return ok;
    }

    public static List<String> getTestSetIds(String xml) {
        Document doc = getDocument(xml);
        NodeList entities = doc.getElementsByTagName("Fields");

        List<String> ids = new LinkedList<>();

        for (int i = 0; i < entities.getLength(); i++) {
            Element element = (Element) (entities.item(i)).getFirstChild();

            if (element.getAttribute("Name").equals("cycle-id")) {
                ids.add(getFieldValue(element));
            }
        }

        return ids;
    }
}
