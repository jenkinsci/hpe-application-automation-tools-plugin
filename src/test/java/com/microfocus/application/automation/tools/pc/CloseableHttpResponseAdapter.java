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
 *  Copyright 2012-2026 Open Text.
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
package com.microfocus.application.automation.tools.pc;

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.util.Locale;

/**
 * Adapter to wrap a BasicHttpResponse into a CloseableHttpResponse.
 * Used by mocks to avoid hitting a real PC server.
 */
public class CloseableHttpResponseAdapter implements CloseableHttpResponse {

    private final BasicHttpResponse response;

    public CloseableHttpResponseAdapter(BasicHttpResponse response) {
        this.response = response;
    }

    @Override
    public void close() throws IOException {
        // nothing to close in the mock
    }

    @Override
    public StatusLine getStatusLine() {
        return response.getStatusLine();
    }

    @Override
    public void setStatusLine(StatusLine statusline) {
        response.setStatusLine(statusline);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code) {
        response.setStatusLine(ver, code);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code, String reason) {
        response.setStatusLine(ver, code, reason);
    }

    @Override
    public void setStatusCode(int code) throws IllegalStateException {
        response.setStatusCode(code);
    }

    @Override
    public void setReasonPhrase(String reason) throws IllegalStateException {
        response.setReasonPhrase(reason);
    }

    @Override
    public HttpEntity getEntity() {
        return response.getEntity();
    }

    @Override
    public void setEntity(HttpEntity entity) {
        response.setEntity(entity);
    }

    @Override
    public Locale getLocale() {
        return response.getLocale();
    }

    @Override
    public void setLocale(Locale loc) {
        response.setLocale(loc);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return response.getProtocolVersion();
    }

    @Override
    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }

    @Override
    public Header[] getHeaders(String name) {
        return response.getHeaders(name);
    }

    @Override
    public Header getFirstHeader(String name) {
        return response.getFirstHeader(name);
    }

    @Override
    public Header getLastHeader(String name) {
        return response.getLastHeader(name);
    }

    @Override
    public Header[] getAllHeaders() {
        return response.getAllHeaders();
    }

    @Override
    public void addHeader(Header header) {
        response.addHeader(header);
    }

    @Override
    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    @Override
    public void setHeader(Header header) {
        response.setHeader(header);
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public void setHeaders(Header[] headers) {
        response.setHeaders(headers);
    }

    @Override
    public void removeHeader(Header header) {
        response.removeHeader(header);
    }

    @Override
    public void removeHeaders(String name) {
        response.removeHeaders(name);
    }

    @Override
    public HeaderIterator headerIterator() {
        return response.headerIterator();
    }

    @Override
    public HeaderIterator headerIterator(String name) {
        return response.headerIterator(name);
    }

    @Override
    public HttpParams getParams() {
        return response.getParams();
    }

    @Override
    public void setParams(HttpParams params) {
        response.setParams(params);
    }
}
