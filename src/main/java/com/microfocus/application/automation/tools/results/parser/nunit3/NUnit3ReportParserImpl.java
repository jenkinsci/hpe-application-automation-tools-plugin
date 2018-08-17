/*
 * Copyright (c) 2012 Hewlett-Packard Development Company, L.P.
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
 */

package com.microfocus.application.automation.tools.results.parser.nunit3;

import com.microfocus.application.automation.tools.results.parser.ReportParseException;
import com.microfocus.application.automation.tools.results.parser.ReportParser;
import com.microfocus.application.automation.tools.results.parser.antjunit.AntJUnitReportParserImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;
import hudson.FilePath;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class NUnit3ReportParserImpl implements ReportParser {

    private static final String TEMP_JUNIT_FILE_PREFIX = "temp-junit";
    private static final String TEMP_JUNIT_FILE_SUFFIX = ".xml";
    private static final String NUNIT_TO_JUNIT_XSLFILE_STR = "nunit-to-junit.xsl";

    private FilePath workspace;

    public NUnit3ReportParserImpl(FilePath workspace) {
        this.workspace = workspace;
    }

    @Override
    public List<AlmTestSet> parseTestSets(InputStream reportInputStream, String testingFramework, String testingTool)
            throws ReportParseException {

        FileOutputStream fileOutputStream = null;
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer nunitTransformer = transformerFactory.newTransformer(
                        new StreamSource(this.getClass().getResourceAsStream(NUNIT_TO_JUNIT_XSLFILE_STR)));
            File junitTargetFile = new File(workspace.createTempFile(TEMP_JUNIT_FILE_PREFIX, TEMP_JUNIT_FILE_SUFFIX).toURI());
            fileOutputStream = new FileOutputStream(junitTargetFile);
            nunitTransformer.transform(new StreamSource(reportInputStream), new StreamResult(fileOutputStream));

            InputStream in = new FileInputStream(junitTargetFile);
            return new AntJUnitReportParserImpl().parseTestSets(in, testingFramework, testingTool);

        } catch (Exception e) {
            throw new ReportParseException(e);

        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    throw new ReportParseException(e);
                }
            }
        }
    }


}
