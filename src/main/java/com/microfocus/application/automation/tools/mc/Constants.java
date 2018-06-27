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

package com.microfocus.application.automation.tools.mc;

/**
 * Created with IntelliJ IDEA.
 * User: jingwei
 * Date: 5/13/16
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class Constants {
    public final static String BOUNDARYSTR = "randomstring";
    public final static String DATA = "data";
    public final static String APP_UPLOAD = "/rest/apps/upload?enforceUpload=true";  // make sure unpacked app is uploaded in case of failure during instrumentation
    public final static String CONTENT_TYPE_DOWNLOAD_VALUE = "multipart/form-data; boundary=----";
    public final static String FILENAME = "filename";
    public static final String LOGIN_SECRET = "x-hp4msecret";
    public static final String SPLIT_COMMA = ";";
    public static final String JSESSIONID = "JSESSIONID";
    public static final String ACCEPT = "Accept";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String COOKIE = "Cookie";
    public static final String SET_COOKIE = "Set-Cookie";
    public static final String EQUAL = "=";
    public static final String LOGIN_URL = "/rest/client/login";
    public static final String CREATE_JOB_URL = "/rest/job/createTempJob";
    public static final String GET_JOB_UEL = "/rest/job/";
    public final static String ICON = "icon";
    public final static String JESEEIONEQ = "JSESSIONID=";
    public final static String TENANT_COOKIE = "TENANT_ID_COOKIE";
    public final static String TENANT_EQ = ";TENANT_ID_COOKIE=";

}
