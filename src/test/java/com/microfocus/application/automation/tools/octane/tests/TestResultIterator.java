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
package com.microfocus.application.automation.tools.octane.tests;

import com.microfocus.application.automation.tools.octane.tests.junit.JUnitTestResult;
import com.microfocus.application.automation.tools.octane.tests.junit.TestResultStatus;
import hudson.util.IOUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class TestResultIterator implements Iterator<JUnitTestResult> {

	private Reader input;
	private XMLEventReader reader;
	private LinkedList<JUnitTestResult> items = new LinkedList<>();
	private boolean closed;
	private String serverId;
	private String jobId;
	private String buildId;
	private String subType;

	public TestResultIterator(Reader input) throws XMLStreamException {
		this.input = input;
		reader = XMLInputFactory.newInstance().createXMLEventReader(input);
	}

	@Override
	public boolean hasNext() {
		try {
			while (items.isEmpty() && !closed) {
				if (reader.hasNext()) {
					XMLEvent event = reader.nextEvent();
					if (event instanceof StartElement) {
						Attribute attribute;
						StartElement element = (StartElement) event;
						String localName = element.getName().getLocalPart();
						if ("test_run".equals(localName)) {
							String moduleName = element.getAttributeByName(new QName("module")).getValue();
							String packageName = element.getAttributeByName(new QName("package")).getValue();
							String className = element.getAttributeByName(new QName("class")).getValue();
							String testName = element.getAttributeByName(new QName("name")).getValue();
							long duration = Long.valueOf(element.getAttributeByName(new QName("duration")).getValue());
							TestResultStatus status = TestResultStatus.fromPrettyName(element.getAttributeByName(new QName("status")).getValue());
							long started = Long.valueOf(element.getAttributeByName(new QName("started")).getValue());
							items.add(new JUnitTestResult(moduleName, packageName, className, testName, status, duration, started, null, null, null,null,null, null, false));
						} else if ("build".equals(localName)) {
							attribute = element.getAttributeByName(new QName("server_id"));
							if (attribute != null) {
								serverId = attribute.getValue();
							}
							attribute = element.getAttributeByName(new QName("job_id"));
							if (attribute != null) {
								jobId = attribute.getValue();
							}
							attribute = element.getAttributeByName(new QName("build_id"));
							if (attribute != null) {
								buildId = attribute.getValue();
							}
							attribute = element.getAttributeByName(new QName("sub_type"));
							if (attribute != null) {
								this.subType = attribute.getValue();
							}
						}
					}
				} else {
					closed = true;
					IOUtils.closeQuietly(input);
					reader.close();
				}
			}
			return !items.isEmpty();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JUnitTestResult next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return items.removeFirst();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public String getServerId() {
		hasNext();
		return serverId;
	}

	public String getJobId() {
		hasNext();
		return jobId;
	}

	public String getBuildId() {
		hasNext();
		return buildId;
	}

	public String getSubType() {
		hasNext();
		return subType;
	}
}
