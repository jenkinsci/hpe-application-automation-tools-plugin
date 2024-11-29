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
package com.microfocus.application.automation.tools.mc;

import com.microfocus.adm.performancecenter.plugins.common.rest.RESTConstants;
import com.microfocus.application.automation.tools.model.AuthModel;
import com.microfocus.application.automation.tools.model.ProxySettings;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * communicate with MC servers, login to MC, upload application to MC server, create job, get job details.
 */
public class JobConfigurationProxy {

    private static final String TOKEN = "token";
    private static final String EXTRA_APPS = "extraApps";
    private static final String INSTRUMENTED = "instrumented";
    private static final String PACKAGED = "Packaged";
    private static final String NOT_PACKAGED = "Not Packaged";
    private static JobConfigurationProxy instance = null;

    private JobConfigurationProxy() {
    }

    public static JobConfigurationProxy getInstance() {
        if (instance == null) {
            instance = new JobConfigurationProxy();
        }
        return instance;
    }

    //Login to MC
    public JSONObject loginToMC(String mcUrl, AuthModel authModel, ProxySettings proxy) {

        JSONObject returnObject = new JSONObject();
        try {
            Map<String, String> headers = getRequestHeaders();

            JSONObject sendObject = new JSONObject();
            if (null == proxy) {
                proxy = new ProxySettings();
            }
            HttpResponse response;
            AuthType authType = authModel.getAuthType();
            if (authType == AuthType.BASE) {
                String tempUsername = authModel.getMcUserName();
                if (!StringUtils.isNullOrEmpty(authModel.getMcTenantId())) {
                    tempUsername += "#" + authModel.getMcTenantId();
                }
                sendObject.put("name", tempUsername);
                sendObject.put("password", authModel.getMcPassword());
                sendObject.put("accountName", "default");
                response = doPost(proxy, mcUrl + Constants.LOGIN_URL, headers, sendObject);
            } else if (Oauth2TokenUtil.isValid(authModel.getMcExecToken())) {
                sendObject = Oauth2TokenUtil.getJSONObject();
                response = doPost(proxy, mcUrl + Constants.LOGIN_URL_OAUTH, headers, sendObject);
            } else {
                System.out.println("ERROR:: oauth token is invalid.");
                return returnObject;
            }
            return parseLoginResponse(response, authType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnObject;
    }

    private JSONObject generateNewToken(String mcUrl, String accessKey, ProxySettings proxy) {

        try {
            Map<String, String> headers = getRequestHeaders();

            if (null == proxy) {
                proxy = new ProxySettings();
            }
            HttpResponse response;
            if (Oauth2TokenUtil.isValid(accessKey)) {
                JSONObject sendObject = Oauth2TokenUtil.getJSONObject();
                String url = mcUrl + Constants.OAUTH_TOKEN_URL;
                response = doPost(proxy, url, headers, sendObject);
                return parseTokenResponse(response, AuthType.TOKEN);
            }
            System.out.println("ERROR: oauth token is invalid.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, String> getRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.ACCEPT, "application/json");
        headers.put(Constants.CONTENT_TYPE, "application/json;charset=UTF-8");
        return headers;
    }
    private JSONObject parseLoginResponse(HttpResponse response, AuthType authType) {
        JSONObject returnObject = new JSONObject();
        if (response == null || response.getHeaders() == null) {
            return null;
        }else if(response.getJsonObject() != null && response.getJsonObject().containsKey("error") && response.getJsonObject().getAsString("error").equals("true")){
            return null;
        }
        Map<String, List<String>> headerFields = response.getHeaders();
        List<String> hp4mSecretList = headerFields.get(Constants.LOGIN_SECRET);
        if (hp4mSecretList != null && !hp4mSecretList.isEmpty() && !StringUtils.isNullOrEmpty(hp4mSecretList.get(0))) {
            returnObject.put(Constants.LOGIN_SECRET, hp4mSecretList.get(0));
        }
        if (authType == AuthType.TOKEN && headerFields.containsKey(Constants.SET_COOKIE)) {
            List<String> cookies = headerFields.get(Constants.SET_COOKIE);
            if (cookies != null && !cookies.isEmpty()) {
                for (String cookie : cookies) {
                    if (cookie.startsWith(Constants.OAUTH2_COOKIE_KEY)) {
                        returnObject.put(Constants.OAUTH2_COOKIE_KEY, getCookieValue(cookie, Constants.OAUTH2_COOKIE_KEY));
                        break;
                    }
                }
            }
        }

        returnObject.put(Constants.COOKIES, response.getCookiesAsString());
        return returnObject;
    }

    private JSONObject parseTokenResponse(HttpResponse response, AuthType authType) {
        JSONObject returnObject = parseLoginResponse(response, authType);
        if (returnObject != null) {
            JSONObject body = response.getJsonObject();
            if (body != null) {
                returnObject.put(Constants.ACCESS_TOKEN, body.getAsString(Constants.ACCESS_TOKEN));
                returnObject.put(Constants.TOKEN_TYPE, body.getAsString(Constants.TOKEN_TYPE));
            }
        }

        return returnObject;
    }

    //check workspace exist or not in MC
    public JSONObject isWorkspaceExist(Map<String, String> headers, String mcUrl, ProxySettings proxy, String appUploadWorkspace) throws IOException {
        if (null == proxy) {
            proxy = new ProxySettings();
        }
        String getWorkspaceUrl = mcUrl + Constants.GET_WORKSPACE_URL;
        if(!StringUtils.isNullOrEmpty(getWorkspaceUrl)){
            getWorkspaceUrl += (String.format("/%s",appUploadWorkspace));
        }
        HttpUtils.ProxyInfo proxyInfo = HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
        HttpResponse response = HttpUtils.doGet(proxyInfo, getWorkspaceUrl, headers, null);
        if (response != null && response.getJsonObject() != null) {
            return response.getJsonObject();
        }
        return null;
    }

    //check signing service
    public String isSigningServiceEnabled(Map<String, String> headers, String mcUrl, ProxySettings proxy) throws IOException {
        if (null == proxy) {
            proxy = new ProxySettings();
        }
        String getAdminSettingUrl = mcUrl + Constants.GET_ADMIN_SETTINGS_URL;
        if(!StringUtils.isNullOrEmpty(getAdminSettingUrl)){
            getAdminSettingUrl += (String.format("/%s","PACKAGING_IOS"));
        }
        HttpUtils.ProxyInfo proxyInfo = HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
        HttpResponse response = HttpUtils.doGet(proxyInfo, getAdminSettingUrl, headers, null);
        if (response != null && response.getJsonArray() != null) {
            for (int i = 0; i < response.getJsonArray().size(); i++) {
                JSONObject setting = (JSONObject) response.getJsonArray().get(i);
                if(setting.getAsString("name").equals("IOS_PACKAGER_ENABLE")){
                    return setting.getAsString("value");
                }
            }
        }
        return null;
    }

    //login into MC server
    public Map<String, String> login(String mcUrl, AuthModel authModel, ProxySettings proxy){
        Map<String, String> headers = new HashMap<>();
        try{
            if (null == proxy) {
                proxy = new ProxySettings();
            }
            JSONObject loginJson = loginToMC(mcUrl, authModel, proxy);
            headers = initHeaders(authModel, loginJson);
        }catch (Exception e) {
            e.printStackTrace();
        }

        return headers;
    }

    //upload app to MC
    public JSONObject upload(Map<String, String> headers, String mcUrl, ProxySettings proxy, String appPath, String appUploadWorkspace) throws IOException {
        File appFile = new File(appPath);
        String uploadUrl = mcUrl + Constants.APP_UPLOAD;
        String signingServiceEnabled = this.isSigningServiceEnabled(headers, mcUrl, proxy);
        if(!StringUtils.isNullOrEmpty(signingServiceEnabled) && signingServiceEnabled.equalsIgnoreCase("true")){
            uploadUrl += "?asyncInstrumentation=false&instrument=true&resign=true&isAppUploadPage=true";
        }else{
            uploadUrl += "?asyncInstrumentation=false&instrument=true&resign=false&isAppUploadPage=true";
        }
        if(!StringUtils.isNullOrEmpty(appUploadWorkspace)){
            uploadUrl += (String.format("&workspaceId=%s",appUploadWorkspace));
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        StringBuilder content = new StringBuilder();
        content.append("\r\n").append("------").append(Constants.BOUNDARYSTR).append("\r\n");
        content.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + appFile.getName() + "\"\r\n");
        content.append("Content-Type: application/octet-stream\r\n\r\n");

        outputStream.write(content.toString().getBytes());

        try (FileInputStream in = new FileInputStream(appFile)) {
            byte[] b = new byte[1024];
            int i = 0;
            while ((i = in.read(b)) != -1) {
                outputStream.write(b, 0, i);
            }
        }

        outputStream.write(("\r\n------" + Constants.BOUNDARYSTR + "--\r\n").getBytes());

        byte[] bytes = outputStream.toByteArray();

        outputStream.close();

        if (null == proxy) {
            proxy = new ProxySettings();
        }

        headers.put(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_DOWNLOAD_VALUE + Constants.BOUNDARYSTR);
        headers.put(Constants.FILENAME, appFile.getName());

        HttpUtils.ProxyInfo proxyInfo = HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
        HttpResponse response = HttpUtils.doPost(proxyInfo, uploadUrl, headers, bytes);

        if (response != null && response.getJsonObject() != null) {
            return response.getJsonObject();
        }
        return null;
    }

    //checking admin setting if prevent uploading to shared assert
    public String isAllowUploadToSharedAssert(Map<String, String> headers, String mcUrl, ProxySettings proxy) throws IOException {
        if (null == proxy) {
            proxy = new ProxySettings();
        }
        String getAdminSettingUrl = mcUrl + Constants.GET_ADMIN_SETTINGS_URL;
        if(!StringUtils.isNullOrEmpty(getAdminSettingUrl)){
            getAdminSettingUrl += (String.format("/%s",Constants.USER_PERMISSION_CATEGORY));
        }
        HttpUtils.ProxyInfo proxyInfo = HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
        HttpResponse response = HttpUtils.doGet(proxyInfo, getAdminSettingUrl, headers, null);
        if (response != null && response.getJsonArray() != null) {
            for (int i = 0; i < response.getJsonArray().size(); i++) {
                JSONObject setting = (JSONObject) response.getJsonArray().get(i);
                if(setting.getAsString("name").equals(Constants.UPLOAD_APP_TO_SHARED_ASSETS)){
                    return setting.getAsString("value");
                }
            }
        }
        return null;
    }

    //get all valid workspaces from app upload
    public JSONArray getAllValidWorkspaces(String mcUrl, AuthModel authModel, ProxySettings proxy) throws IOException {
        try {
            Map<String, String> headers = login(mcUrl, authModel, proxy);
            HttpUtils.ProxyInfo proxyInfo = proxy == null ? null : HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
            String queryString = "includeSharedAssets=true";
            //Default value is true, so only update when setting exist and value is false.
            String strAllowUploadToSharedAssert = isAllowUploadToSharedAssert(headers,mcUrl,proxy);
            if(!StringUtils.isNullOrEmpty(strAllowUploadToSharedAssert) && strAllowUploadToSharedAssert.equalsIgnoreCase("false")){
                queryString = "includeSharedAssets=false";
            }
            HttpResponse response = HttpUtils.doGet(proxyInfo, mcUrl + Constants.GET_ALL_WORKSPACES_URL, headers, queryString);
            if (response != null && response.getJsonArray() != null) {
                return response.getJsonArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String createTempJobWithHeaders (String mcUrl, ProxySettings proxy, Map<String, String> headers) {
            if (headers != null) {
                HttpUtils.ProxyInfo proxyInfo = proxy == null ? null : HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
                HttpResponse response = HttpUtils.doGet(proxyInfo, mcUrl + Constants.CREATE_JOB_URL, headers, null);

                if (response != null && response.getJsonObject() != null) {
                    JSONObject job = response.getJsonObject();
                    if (job != null && job.get("data") != null) {
                        JSONObject data = (JSONObject) job.get("data");
                        return data.getAsString("id");
                    }
                }
            }

        return null;
    }

    //get one job by id
    public JSONObject getJobById(String mcUrl, AuthModel authModel, ProxySettings proxy, String jobUUID) {
        JSONObject jobJsonObject = null;
        try {
            JSONObject loginJson = loginToMC(mcUrl, authModel, proxy);

            Map<String, String> headers = initHeaders(authModel,loginJson);
            jobJsonObject = getJobByIdWithHeaders(mcUrl, proxy, jobUUID, headers);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return removeIcon(jobJsonObject);
    }

    public JSONObject getJobByIdWithHeaders(String mcUrl, ProxySettings proxy, String jobUUID, Map<String, String> headers) {
        JSONObject jobJsonObject = null;
        if (!StringUtils.isNullOrEmpty(jobUUID) && headers != null) {
            HttpUtils.ProxyInfo proxyInfo = proxy == null ? null : HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
            HttpResponse response = HttpUtils.doGet(proxyInfo, mcUrl + Constants.GET_JOB_UEL + jobUUID, headers, null);

            if (response != null && response.getJsonObject() != null) {
                jobJsonObject = response.getJsonObject();
            }
            if (jobJsonObject != null) {
                jobJsonObject = (JSONObject) jobJsonObject.get(Constants.DATA);
            }
        }

        return jobJsonObject;
    }

    public JSONObject getBrowserLab(String mcUrl, String accessKey, ProxySettings proxy) {
        JSONObject jsonObject = null;

        try {
            JSONObject loginJson = generateNewToken(mcUrl, accessKey, proxy);
            if (loginJson == null) {
                return null;
            }
            String hp4mSecret = loginJson.getAsString(Constants.LOGIN_SECRET);
            String token = loginJson.getAsString(Constants.ACCESS_TOKEN);
            String tokenType = loginJson.getAsString(Constants.TOKEN_TYPE);
            String cookies = loginJson.getAsString(Constants.COOKIES);
            if (thereIsNoArgumentNullOrEmpty(hp4mSecret, token, tokenType, cookies)) {
                Map<String, String> headers = new HashMap<>();
                headers.put(Constants.ACCEPT, "application/json");
                headers.put(Constants.LOGIN_SECRET, hp4mSecret);
                headers.put(Constants.COOKIE, cookies);
                headers.put(RESTConstants.AUTHORIZATION, String.format("%s %s", tokenType, token));
                HttpUtils.ProxyInfo proxyInfo = proxy == null ? null : HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
                String url = String.format("%s%s?toolVersion=23.4", mcUrl, Constants.GET_BROWSER_LAB_URL);
                HttpResponse response = HttpUtils.doGet(proxyInfo, url, headers, null);

                if (response != null && response.getJsonObject() != null) {
                    jsonObject = response.getJsonObject();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    //parse one job.and get the data we want
    public JSONObject getJobJSONData(String mcUrl, AuthModel authModel, ProxySettings proxy, String jobUUID) {
        JSONObject jobJSON = getJobById(mcUrl, authModel, proxy, jobUUID);

        JSONObject returnJSON = new JSONObject();
        if (jobJSON == null) {
            return returnJSON;
        }
        //Device Capabilities
        JSONObject returnDeviceCapabilityJSON = new JSONObject();

        JSONObject detailJSON = (JSONObject) jobJSON.get("capableDeviceFilterDetails");
        if (detailJSON != null) {
            String osType = (String) detailJSON.get("platformName");
            String osVersion = (String) detailJSON.get("platformVersion");
            String manufacturerAndModel = (String) detailJSON.get("deviceName");
            String targetLab = (String) detailJSON.get("source");

            returnDeviceCapabilityJSON.put("OS", osType + osVersion);
            returnDeviceCapabilityJSON.put("manufacturerAndModel", manufacturerAndModel);
            returnDeviceCapabilityJSON.put("targetLab", targetLab);
        }

        JSONObject returnDeviceJSON = new JSONObject();
        //specific device
        JSONArray devices = (JSONArray) jobJSON.get("devices");

        if (devices != null) {
            JSONObject deviceJSON = (JSONObject) devices.get(0);
            if (deviceJSON != null) {
                String deviceID = deviceJSON.getAsString("deviceID");
                String osType = deviceJSON.getAsString("osType");
                String osVersion = deviceJSON.getAsString("osVersion");
                String manufacturerAndModel = deviceJSON.getAsString("model");

                returnDeviceJSON.put("deviceId", deviceID);
                returnDeviceJSON.put("OS", osType + " " + osVersion);
                returnDeviceJSON.put("manufacturerAndModel", manufacturerAndModel);
            }
        }
        //Applications under test
        JSONArray returnExtraJSONArray = new JSONArray();
        StringBuilder extraApps = new StringBuilder();
        JSONArray extraAppJSONArray = (JSONArray) jobJSON.get(EXTRA_APPS);

        if (extraAppJSONArray != null) {
            Iterator<Object> iterator = extraAppJSONArray.iterator();

            while (iterator.hasNext()) {
                JSONObject extraAPPJSON = new JSONObject();

                JSONObject nextJSONObject = (JSONObject) iterator.next();
                String extraAppName = (String) nextJSONObject.get("name");
                Boolean instrumented = (Boolean) nextJSONObject.get(INSTRUMENTED);

                extraAPPJSON.put("extraAppName", extraAppName);
                extraAPPJSON.put(INSTRUMENTED, instrumented ? PACKAGED : NOT_PACKAGED);
                if (extraApps.length() > 1) {
                    extraApps.append(";\n");
                }
                extraApps.append(extraAppName).append("\t\t").append(instrumented ? PACKAGED : NOT_PACKAGED);

                returnExtraJSONArray.add(extraAPPJSON);
            }
        }
        //Test Definitions
        JSONObject returnDefinitionJSON = new JSONObject();

        JSONObject applicationJSON = (JSONObject) jobJSON.get("application");

        if (applicationJSON != null) {
            String launchApplicationName = (String) applicationJSON.get("name");
            Boolean instrumented = (Boolean) applicationJSON.get(INSTRUMENTED);

            returnDefinitionJSON.put("launchApplicationName", launchApplicationName);
            returnDefinitionJSON.put(INSTRUMENTED, instrumented ? PACKAGED : NOT_PACKAGED);
        }

        //Device metrics,Install Restart
        String headerStr = (String) jobJSON.get("header");
        JSONObject headerJSON = parseJSONString(headerStr);
        if (headerJSON != null) {
            JSONObject configurationJSONObject = (JSONObject) headerJSON.get("configuration");
            Boolean restart = (Boolean) configurationJSONObject.get("restartApp");
            Boolean install = (Boolean) configurationJSONObject.get("installAppBeforeExecution");
            Boolean uninstall = (Boolean) configurationJSONObject.get("deleteAppAfterExecution");

            StringBuilder sb = new StringBuilder("");
            if (restart) {
                sb.append("Restart;");
            }
            if (install) {
                sb.append("Install;");
            }
            if (uninstall) {
                sb.append("Uninstall;");
            }
            JSONObject collectJSON = (JSONObject) headerJSON.get("collect");
            StringBuilder deviceMetricsSb = new StringBuilder("");
            //device metrics
            if (collectJSON != null) {
                Boolean useCPU = (Boolean) collectJSON.get("cpu");
                Boolean useMemory = (Boolean) collectJSON.get("memory");
                Boolean useLogs = (Boolean) collectJSON.get("logs");
                Boolean useScreenshot = (Boolean) collectJSON.get("screenshot");
                Boolean useFreeMemory = (Boolean) collectJSON.get("freeMemory");
                if (useCPU) {
                    deviceMetricsSb.append("CPU;");
                }
                if (useMemory) {
                    deviceMetricsSb.append("Memory;");
                }
                if (useLogs) {
                    deviceMetricsSb.append("Log;");
                }
                if (useScreenshot) {
                    deviceMetricsSb.append("Screenshot;");
                }
                if (useFreeMemory) {
                    deviceMetricsSb.append("FreeMomery;");
                }
            }
            returnDefinitionJSON.put("autActions", removeLastSemicolon(sb));
            returnDefinitionJSON.put("deviceMetrics", removeLastSemicolon(deviceMetricsSb));
        }
        returnJSON.put("deviceCapability", returnDeviceCapabilityJSON);
        returnJSON.put(EXTRA_APPS, extraApps.toString());
        returnJSON.put("extraApps2", returnExtraJSONArray);
        returnJSON.put("definitions", returnDefinitionJSON);
        returnJSON.put("jobUUID", jobUUID);
        returnJSON.put("deviceJSON", returnDeviceJSON);
        return returnJSON;
    }

    public boolean isServerOnSaaS(Map<String, String> headers, String mcUrl, ProxySettings proxy) {
        if (null == proxy) {
            proxy = new ProxySettings();
        }
        String isDLServerOnSaaSURL = mcUrl + Constants.IS_DL_SERVER_ON_SAAS_URL;
        HttpUtils.ProxyInfo proxyInfo = HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword());
        HttpResponse response = HttpUtils.doGet(proxyInfo, isDLServerOnSaaSURL, headers, null);
        if (response.getJsonObject() != null ) {
            boolean isError = Boolean.parseBoolean(response.getJsonObject().get("error").toString());
            return !isError;

        }

        return false;
    }

    private JSONObject parseJSONString(String jsonString) {
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) JSONValue.parseStrict(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private String getCookieValue(String setCookie, String cookieName) {
        if (StringUtils.isNullOrEmpty(setCookie)) {
            return null;
        }
        String id = null;
        String[] cookies = setCookie.split(Constants.SPLIT_COMMA);
        for (int i = 0; i < cookies.length; i++) {
            if (cookies[i].contains(cookieName)) {
                int index = cookies[i].indexOf(Constants.EQUAL);
                id = cookies[i].substring(index + 1);
                break;
            }
        }
        return id;
    }

    private boolean thereIsNoArgumentNullOrEmpty(String... args) {
        for (String arg : args) {
            if (StringUtils.isNullOrEmpty(arg)) {
                return false;
            }
        }
        return true;
    }

    private String removeLastSemicolon(StringBuilder sb) {
        String result = sb.toString();
        int indexOf = result.lastIndexOf(";");
        if (indexOf > 0) {
            result = result.substring(0, indexOf);
        }
        return result;
    }

    private JSONObject removeIcon(JSONObject jobJSON) {
        if (jobJSON != null) {
            JSONObject applicationJSONObject = (JSONObject) jobJSON.get("application");
            if (applicationJSONObject != null) {
                applicationJSONObject.remove(Constants.ICON);
            }
            JSONArray extArr = (JSONArray) jobJSON.get(EXTRA_APPS);
            if (extArr != null) {
                Iterator<Object> iterator = extArr.iterator();
                while (iterator.hasNext()) {
                    JSONObject extAppJSONObject = (JSONObject) iterator.next();
                    extAppJSONObject.remove(Constants.ICON);
                }
            }
        }
        return jobJSON;
    }


    public Map<String, String> initHeaders(AuthModel authModel, JSONObject loginJson) {
        Map<String, String> headers = new HashMap<>();
        if (loginJson != null) {
            String hp4mSecret = loginJson.getAsString(Constants.LOGIN_SECRET);
            List<String> args = new ArrayList<>();
            args.add(hp4mSecret);
            String oauth2 = "", jSessionId = "";
            if (TOKEN.equals(authModel.getValue())) {
                oauth2 = loginJson.getAsString(Constants.OAUTH2_COOKIE_KEY);
                if (StringUtils.isNullOrEmpty(oauth2)) {
                    System.out.println("ERROR:: loginToMC failed with null oauth cookie.");
                    return null;
                }
                args.add(oauth2);
            } else {
                jSessionId = loginJson.getAsString(Constants.JSESSIONID);
                if (!StringUtils.isNullOrEmpty(jSessionId))
                    args.add(jSessionId);
            }

            if (thereIsNoArgumentNullOrEmpty(args.toArray(new String[0]))) {
                headers.put(Constants.LOGIN_SECRET, hp4mSecret);
                if (TOKEN.equals(authModel.getValue())) {
                    headers.put(Constants.COOKIE, String.format("%s=%s;", Constants.OAUTH2_COOKIE_KEY, oauth2));
                } else if (!StringUtils.isNullOrEmpty(jSessionId)) {
                    headers.put(Constants.COOKIE, String.format("%s=%s;", Constants.JSESSIONID, jSessionId));
                } else if (loginJson.containsKey(Constants.COOKIES)) {
                    headers.put(Constants.COOKIE, loginJson.getAsString(Constants.COOKIES));
                }
                return headers;
            }
        }
        return null;
    }

    private HttpResponse doPost(ProxySettings proxy, String url, Map<String, String> headers, JSONObject body) {
        return HttpUtils.doPost(HttpUtils.setProxyCfg(proxy.getFsProxyAddress(), proxy.getFsProxyUserName(), proxy.getFsProxyPassword()), url, headers, body.toJSONString().getBytes(), true);
    }
}
