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
package com.microfocus.application.automation.tools.results.parser.nunit3;

import com.microfocus.application.automation.tools.results.parser.ReportParseException;
import com.microfocus.application.automation.tools.results.parser.ReportParser;
import com.microfocus.application.automation.tools.results.parser.antjunit.AntJUnitReportParserImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;
import hudson.FilePath;

import javax.xml.XMLConstants;
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

/**
 * NUnit3 Report Parser implement.
 * It will convert Nunit 3 report to Junit report with xsl then start AntJunit parser.
 */
public class NUnit3ReportParserImpl implements ReportParser {

    private static final String TEMP_JUNIT_FILE_PREFIX = "temp-junit";
    private static final String TEMP_JUNIT_FILE_SUFFIX = ".xml";
    private static final String NUNIT_TO_JUNIT_XSLFILE = "nunit-to-junit.xsl";

    private FilePath workspace;

    public NUnit3ReportParserImpl(FilePath workspace) {
        this.workspace = workspace;
    }

    @Override
    public List<AlmTestSet> parseTestSets(InputStream reportInputStream, String testingFramework, String testingTool)
            throws ReportParseException {

        // Use the xsl to convert the nunit3 and nunit to junit and then parse with junit logic.
        // This can be extended to cover all kinds of result format.
        // When new format comes, only need to provide a xsl, no need to change any code.

        FileOutputStream fileOutputStream = null;
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer nunitTransformer = transformerFactory.newTransformer(
                        new StreamSource(this.getClass().getResourceAsStream(NUNIT_TO_JUNIT_XSLFILE)));
            File junitTargetFile = new File(workspace.createTempFile(TEMP_JUNIT_FILE_PREFIX, TEMP_JUNIT_FILE_SUFFIX).toURI());
            fileOutputStream = new FileOutputStream(junitTargetFile);
            nunitTransformer.transform(new StreamSource(reportInputStream), new StreamResult(fileOutputStream));

            InputStream in = new FileInputStream(junitTargetFile);
            return new AntJUnitReportParserImpl().parseTestSets(in, testingFramework, testingTool);

        } catch (Exception e) {
            throw new ReportParseException(e);

        } finally {
            try {
                if (reportInputStream != null) {
                    reportInputStream.close();
                }
            } catch (IOException e) {
                throw new ReportParseException(e);
            }

            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                throw new ReportParseException(e);
            }
        }
    }
}
