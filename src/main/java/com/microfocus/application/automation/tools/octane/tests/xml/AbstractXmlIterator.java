/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2024 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.tests.xml;

import com.ctc.wstx.stax.WstxInputFactory;
import hudson.util.IOUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public abstract class AbstractXmlIterator<E> {

    private InputStream is;
    protected XMLEventReader reader;
    private LinkedList<E> queue;
    private boolean closed;

    public AbstractXmlIterator(InputStream is) throws XMLStreamException {
        this.is = is;
        reader = createXmlInputFactory().createXMLEventReader(is);
        queue = new LinkedList<>();
    }

    public XMLEvent peek() throws XMLStreamException {
        return reader.peek();
    }

    public boolean hasNext() throws XMLStreamException, IOException, InterruptedException {
        while (queue.isEmpty() && !closed) {
            if (reader.hasNext()) {
                onEvent(reader.nextEvent());
            } else {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    // close quietly
                }
                IOUtils.closeQuietly(is);
                closed = true;
            }
        }
        return !queue.isEmpty();
    }

    public E next() throws XMLStreamException, IOException, InterruptedException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        } else {
            return queue.removeFirst();
        }
    }

    protected abstract void onEvent(XMLEvent event) throws XMLStreamException, IOException, InterruptedException;

    protected void addItem(E item) {
        queue.add(item);
    }

    protected String readNextValue() throws XMLStreamException {
        XMLEvent nextEvent = reader.nextEvent();
        if(nextEvent instanceof EndElement){
            return "";
        } else {
            return ((Characters)nextEvent).getData();
        }
    }

    protected String peekNextValue() throws XMLStreamException {
        XMLEvent event = reader.peek();
        if(event instanceof EndElement){
            return "";
        } else {
            return ((Characters)event).getData();
        }
    }

    private static XMLInputFactory createXmlInputFactory() {
        //up to Jenkins version 2.319.2 this was the XML Input Factory implementation used, new Factory cause test result corruption
        XMLInputFactory xmlFactory = new WstxInputFactory();
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return xmlFactory;
    }
}
