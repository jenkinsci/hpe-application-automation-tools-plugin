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

package com.microfocus.application.automation.tools.sse.sdk;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * This is a naive implementation of an HTTP response. We use it to simplify matters in the
 * examples. It is nothing more than a container of the response headers and the response body.
 */
public class Response {
    
    private Map<String, List<String>> _headers;
    private byte[] _data;
    private Throwable _failure;
    private int _statusCode = -1;
    
    public Response() {
        
        this(null, null, null, -1);
    }
    
    public Response(Exception failure) {
        
        this(null, null, failure, -1);
    }
    
    public Response(
            Map<String, List<String>> headers,
            byte[] data,
            Exception failure,
            int statusCode) {
        
        _headers = headers;
        _data = data;
        _failure = failure;
        _statusCode = statusCode;
    }
    
    public Map<String, List<String>> getHeaders() {
        
        return _headers;
    }
    
    public void setHeaders(Map<String, List<String>> responseHeaders) {
        
        _headers = responseHeaders;
    }
    
    public byte[] getData() {
        
        return _data;
    }
    
    public void setData(byte[] data) {
        
        _data = data;
    }
    
    /**
     * @return the failure if the access to the requested URL failed, such as a 404 or 500. If no
     *         such failure occurred this method returns null.
     */
    public Throwable getFailure() {
        
        return _failure;
    }
    
    public void setFailure(Throwable cause) {
        
        this._failure = cause;
    }
    
    public int getStatusCode() {
        
        return _statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        
        _statusCode = statusCode;
    }
    
    public boolean isOk() {
        
        return getFailure() == null
               && (getStatusCode() == HttpURLConnection.HTTP_OK
                   || getStatusCode() == HttpURLConnection.HTTP_CREATED || getStatusCode() == HttpURLConnection.HTTP_ACCEPTED);
    }
    
    /**
     * @see Object#toString() return the contents of the byte[] data as a string.
     */
    @Override
    public String toString() {
        
        return new String(_data);
    }
}
