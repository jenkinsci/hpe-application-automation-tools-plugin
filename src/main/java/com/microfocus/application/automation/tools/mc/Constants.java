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
    public final static String APP_UPLOAD = "/rest/v2/apps";  // make sure unpacked app is uploaded in case of failure during instrumentation
    public final static String CONTENT_TYPE_DOWNLOAD_VALUE = "multipart/form-data; boundary=----";
    public final static String FILENAME = "filename";
    public static final String LOGIN_SECRET = "x-hp4msecret";
    public static final String SPLIT_COMMA = ";";
    public static final String JSESSIONID = "JSESSIONID";
    public static final String LWSSO_COOKIE_KEY = "LWSSO_COOKIE_KEY";
    public static final String OAUTH2_COOKIE_KEY = "OAUTH2_COOKIE_KEY";
    public static final String TENANT_ID_COOKIE = "TENANT_ID_COOKIE";
    public static final String ACCEPT = "Accept";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String COOKIE = "Cookie";
    public static final String COOKIES = "Cookies";
    public static final String SET_COOKIE = "Set-Cookie";
    public static final String EQUAL = "=";
    public static final String LOGIN_URL = "/rest/client/login";
    public static final String LOGIN_URL_OAUTH = "/rest/client/v2/oauth2/login";
    public static final String OAUTH_TOKEN_URL = "/rest/oauth2/token";
    public static final String CREATE_JOB_URL = "/rest/job/createTempJob";
    public static final String GET_JOB_UEL = "/rest/job/";
    public static final String GET_ALL_WORKSPACES_URL = "/rest/v2/workspaces";
    public static final String GET_WORKSPACE_URL = "/rest/v2/workspaces";
    public static final String GET_ADMIN_SETTINGS_URL = "/rest/v2/adminSettings";
    public static final String GET_BROWSER_LAB_URL = "/rest/v2/browser-lab/uftone/templates";
    public static final String IS_DL_SERVER_ON_SAAS_URL = "/rest/v2/status/saas";
    public final static String ICON = "icon";
    public final static String JSESSIONID_EQ = "JSESSIONID=";
    public final static String TENANT_COOKIE = "TENANT_ID_COOKIE";
    public final static String ACCESS_TOKEN = "access_token";
    public  final static String TOKEN_TYPE = "token_type";
    public final static String SHARED_ASSETS = "Shared assets";
    public final static String USER_PERMISSION_CATEGORY ="USER_PERMISSION";
    public final static String UPLOAD_APP_TO_SHARED_ASSETS = "USER_PERMISSION_UPLOAD_APP_TO_SHARED_ASSETS";
}
