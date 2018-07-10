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

import hudson.FilePath;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.util.StringUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Utility for extracting information from UFT test located on FS
 */
public class UFTTestUtil {

    /**
     * Extract test description from UFT GUI test.
     * Note : UFT API test doesn't contain description
     * @param dirPath path of UFT test
     * @return test description
     */
    public static String getTestDescription(FilePath dirPath) {
        String desc;

        try {
            if(!dirPath.exists()){
                return  null;
            }

            FilePath tspTestFile = new FilePath(dirPath, "Test.tsp");
            InputStream is = new FileInputStream(tspTestFile.getRemote());
            String xmlContent = decodeXmlContent(is);

            SAXBuilder saxBuilder = new SAXBuilder(XMLReaders.NONVALIDATING, null, null);
            Document document = saxBuilder.build(new StringReader(xmlContent));
            Element rootElement = document.getRootElement();
            Element descElement = rootElement.getChild("Description");
            desc = descElement.getValue();
        } catch (Exception e) {
            return null;
        }

        return desc;
    }

    public static String decodeXmlContent(InputStream stream) throws IOException {
        POIFSFileSystem poiFS = new POIFSFileSystem(stream);
        DirectoryNode root = poiFS.getRoot();
        String xmlData = "";

        for (Entry entry : root) {
            String name = entry.getName();
            if ("ComponentInfo".equals(name)) {
                if (entry instanceof DirectoryEntry) {
                    System.out.println(entry);
                } else if (entry instanceof DocumentEntry) {
                    byte[] content = new byte[((DocumentEntry) entry).getSize()];
                    poiFS.createDocumentInputStream("ComponentInfo").read(content);
                    String fromUnicodeLE = StringUtil.getFromUnicodeLE(content);
                    xmlData = fromUnicodeLE.substring(fromUnicodeLE.indexOf('<')).replaceAll("\u0000", "");
                }
            }
        }
        return xmlData;
    }
}
