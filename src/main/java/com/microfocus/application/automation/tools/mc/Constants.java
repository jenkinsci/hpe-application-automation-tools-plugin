/*
 *
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
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
