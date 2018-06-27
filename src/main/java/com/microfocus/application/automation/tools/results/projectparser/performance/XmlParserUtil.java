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

package com.microfocus.application.automation.tools.results.projectparser.performance;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.annotation.Nullable;

/**
 * Created by kazaky on 17/10/2016.
 */
public class XmlParserUtil {

  @Nullable
  public static Node getNode(String tagName, NodeList nodes) {
    for (int x = 0; x < nodes.getLength(); x++) {
      Node node = nodes.item(x);
      if (node.getNodeName().equalsIgnoreCase(tagName)) {
        return node;
      }
    }
    return null;
  }

  public static String getNodeValue(Node node) {
    NodeList childNodes = node.getChildNodes();
    for (int x = 0; x < childNodes.getLength(); x++) {
      Node data = childNodes.item(x);
      if (data.getNodeType() == Node.TEXT_NODE)
        return data.getNodeValue();
    }
    return "";
  }

  public static String getNodeValue(String tagName, NodeList nodes) {
    for (int x = 0; x < nodes.getLength(); x++) {
      Node node = nodes.item(x);
      if (node.getNodeName().equalsIgnoreCase(tagName)) {
        NodeList childNodes = node.getChildNodes();
        for (int y = 0; y < childNodes.getLength(); y++) {
          Node data = childNodes.item(y);
          if (data.getNodeType() == Node.TEXT_NODE)
            return data.getNodeValue();
        }
      }
    }
    return "";
  }

  public static String getNodeAttr(String attrName, Node node) {
    NamedNodeMap attrs = node.getAttributes();
    for (int y = 0; y < attrs.getLength(); y++) {
      Node attr = attrs.item(y);
      if (attr.getNodeName().equalsIgnoreCase(attrName)) {
        return attr.getNodeValue();
      }
    }
    return "";
  }

  public static String getNodeAttr(String tagName, String attrName, NodeList nodes) {
    for (int x = 0; x < nodes.getLength(); x++) {
      Node node = nodes.item(x);
      if (node.getNodeName().equalsIgnoreCase(tagName)) {
        NodeList childNodes = node.getChildNodes();
        for (int y = 0; y < childNodes.getLength(); y++) {
          Node data = childNodes.item(y);
          if ((data.getNodeType() == Node.ATTRIBUTE_NODE) && data.getNodeName().equalsIgnoreCase(attrName)) {
              return data.getNodeValue();
            }
          }
        }
      }
    return "";
  }
}
