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
