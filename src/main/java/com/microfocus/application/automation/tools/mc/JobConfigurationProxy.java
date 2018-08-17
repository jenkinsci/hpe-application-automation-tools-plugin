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

import com.microfocus.application.automation.tools.sse.common.StringUtils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * communicate with MC servers, login to MC, upload application to MC server, create job, get job details.
 */
public class JobConfigurationProxy {

    private static JobConfigurationProxy instance = null;

    private JobConfigurationProxy() {
    }

    public static JobConfigurationProxy getInstance(){
        if(instance == null){
            instance = new JobConfigurationProxy();
        }
        return instance;
    }
    //Login to MC
    public JSONObject loginToMC(String mcUrl, String mcUserName, String mcPassword, String mcTenantId,
                                String proxyAddress, String proxyUsername, String proxyPassword) {

        JSONObject returnObject = new JSONObject();
        try {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put(Constants.ACCEPT, "application/json");
            headers.put(Constants.CONTENT_TYPE, "application/json;charset=UTF-8");
//            headers.put("TENANT_ID_COOKIE", mcTenantId);

            JSONObject sendObject = new JSONObject();
            if(!StringUtils.isNullOrEmpty(mcTenantId)){
                mcUserName = mcUserName + "#" + mcTenantId;
            }
            sendObject.put("name", mcUserName);
            sendObject.put("password", mcPassword);
            sendObject.put("accountName", "default");
            HttpResponse response = HttpUtils.post(HttpUtils.setProxyCfg(proxyAddress, proxyUsername, proxyPassword), mcUrl + Constants.LOGIN_URL, headers, sendObject.toJSONString().getBytes());

            if (response != null && response.getHeaders() != null) {
                Map<String, List<String>> headerFields = response.getHeaders();
                List<String> hp4mSecretList = headerFields.get(Constants.LOGIN_SECRET);
                String hp4mSecret = null;
                if (hp4mSecretList != null && hp4mSecretList.size() != 0) {
                    hp4mSecret = hp4mSecretList.get(0);
                }
                List<String> setCookieList = headerFields.get(Constants.SET_COOKIE);
                String setCookie = null;
                String tenantCookie = null;
                if (setCookieList != null && setCookieList.size() != 0) {
                    setCookie = setCookieList.get(0);
                    for(String str : setCookieList){
                        if(str.contains(Constants.JSESSIONID) && str.startsWith(Constants.JSESSIONID)){
                            setCookie = str;
                            continue;
                        }else if(str.contains(Constants.TENANT_COOKIE) && str.startsWith(Constants.TENANT_COOKIE)){
                            tenantCookie = str;
                            continue;
                        }
                    }
                }
                String jsessionId = getCookieValue(setCookie, Constants.JSESSIONID);
                String tenantId = getCookieValue(tenantCookie, Constants.TENANT_COOKIE);
                returnObject.put(Constants.JSESSIONID, jsessionId);
                returnObject.put(Constants.TENANT_COOKIE, tenantId);
                returnObject.put(Constants.LOGIN_SECRET, hp4mSecret);
                String cookies = Constants.JESEEIONEQ + jsessionId;
                if(!StringUtils.isNullOrEmpty(tenantId)){
                    cookies = cookies + Constants.TENANT_EQ + tenantId;
                }
                returnObject.put(Constants.COOKIE, cookies);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnObject;
    }

    //upload app to MC
    public JSONObject upload(String mcUrl, String mcUserName, String mcPassword, String mcTenantId,
                             String proxyAddress, String proxyUsername, String proxyPassword, String appPath) throws Exception {

        JSONObject json = null;
        String hp4mSecret = null;
        String jsessionId = null;

        File appFile = new File(appPath);

        String uploadUrl = mcUrl + Constants.APP_UPLOAD;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        StringBuffer content = new StringBuffer();
        content.append("\r\n").append("------").append(Constants.BOUNDARYSTR).append("\r\n");
        content.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + appFile.getName() + "\"\r\n");
        content.append("Content-Type: application/octet-stream\r\n\r\n");

        outputStream.write(content.toString().getBytes());

        FileInputStream in = new FileInputStream(appFile);
        byte[] b = new byte[1024];
        int i = 0;
        while ((i = in.read(b)) != -1) {
            outputStream.write(b, 0, i);
        }
        in.close();

        outputStream.write(("\r\n------" + Constants.BOUNDARYSTR + "--\r\n").getBytes());

        byte[] bytes = outputStream.toByteArray();

        outputStream.close();

        JSONObject loginJson = loginToMC(mcUrl, mcUserName, mcPassword, mcTenantId, proxyAddress, proxyUsername, proxyPassword);

        if (loginJson != null) {
            hp4mSecret = (String) loginJson.get(Constants.LOGIN_SECRET);
            jsessionId = (String) loginJson.get(Constants.JSESSIONID);
        }
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.LOGIN_SECRET, hp4mSecret);
        headers.put(Constants.COOKIE, Constants.JESEEIONEQ + jsessionId);
        headers.put(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_DOWNLOAD_VALUE + Constants.BOUNDARYSTR);
        headers.put(Constants.FILENAME, appFile.getName());

        HttpUtils.ProxyInfo proxyInfo = HttpUtils.setProxyCfg(proxyAddress, proxyUsername, proxyPassword);
        HttpResponse response = HttpUtils.post(proxyInfo, uploadUrl, headers, bytes);

        if (response != null && response.getJsonObject() != null) {
            json = response.getJsonObject();
        }
        return json;
    }

    //create one temp job
    public String createTempJob(String mcUrl, String mcUserName, String mcPassword, String mcTenantId, String proxyAddress, String proxyUserName, String proxyPassword) {
        JSONObject job = null;
        String jobId = null;
        String hp4mSecret = null;
        String jsessionId = null;

        String loginJson = loginToMC(mcUrl, mcUserName, mcPassword, mcTenantId, proxyAddress, proxyUserName, proxyPassword).toJSONString();
        try {
            if (loginJson != null) {
                JSONObject jsonObject = (JSONObject) JSONValue.parseStrict(loginJson);
                hp4mSecret = (String) jsonObject.get(Constants.LOGIN_SECRET);
                jsessionId = (String) jsonObject.get(Constants.JSESSIONID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isValid = argumentsCheck(hp4mSecret, jsessionId);

        if (isValid) {
            try {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put(Constants.LOGIN_SECRET, hp4mSecret);
                headers.put(Constants.COOKIE, Constants.JESEEIONEQ + jsessionId);
                HttpResponse response = HttpUtils.get(HttpUtils.setProxyCfg(proxyAddress,proxyUserName,proxyPassword), mcUrl + Constants.CREATE_JOB_URL, headers, null);

                if (response != null && response.getJsonObject() != null) {
                    job = response.getJsonObject();
                    if(job != null && job.get("data") != null){
                        JSONObject data = (JSONObject)job.get("data");
                        jobId = data.getAsString("id");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jobId;
    }

    //get one job by id
    public JSONObject getJobById(String mcUrl, String mcUserName, String mcPassword, String mcTenantId, String proxyAddress, String proxyUsername, String proxyPassword, String jobUUID) {
        JSONObject jobJsonObject = null;
        String hp4mSecret = null;
        String jsessionId = null;

        String loginJson = loginToMC(mcUrl, mcUserName, mcPassword, mcTenantId, proxyAddress, proxyUsername, proxyPassword).toJSONString();
        try {
            if (loginJson != null) {
                JSONObject jsonObject = (JSONObject) JSONValue.parseStrict(loginJson);
                hp4mSecret = (String) jsonObject.get(Constants.LOGIN_SECRET);
                jsessionId = (String) jsonObject.get(Constants.JSESSIONID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean b = argumentsCheck(jobUUID, hp4mSecret, jsessionId);

        if (b) {
            try {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put(Constants.LOGIN_SECRET, hp4mSecret);
                headers.put(Constants.COOKIE, Constants.JESEEIONEQ + jsessionId);
                HttpResponse response = HttpUtils.get(HttpUtils.setProxyCfg(proxyAddress, proxyUsername, proxyPassword), mcUrl + Constants.GET_JOB_UEL + jobUUID, headers, null);

                if (response != null && response.getJsonObject() != null) {
                    jobJsonObject = response.getJsonObject();
                }
                if(jobJsonObject != null){
                    jobJsonObject = (JSONObject)jobJsonObject.get(Constants.DATA);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return removeIcon(jobJsonObject);
    }

    //parse one job.and get the data we want
    public JSONObject getJobJSONData(String mcUrl, String mcUserName, String mcPassword, String mcTenantId, String proxyAddress, String proxyUserName, String proxyPassword, String jobUUID) {
        JSONObject jobJSON = getJobById(mcUrl, mcUserName, mcPassword, mcTenantId, proxyAddress, proxyUserName, proxyPassword, jobUUID);

        JSONObject returnJSON = new JSONObject();

        //Device Capabilities
        if (jobJSON != null) {
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
            JSONArray extraAppJSONArray = (JSONArray) jobJSON.get("extraApps");

            if (extraAppJSONArray != null) {
                Iterator<Object> iterator = extraAppJSONArray.iterator();

                while (iterator.hasNext()) {

                    JSONObject extraAPPJSON = new JSONObject();

                    JSONObject nextJSONObject = (JSONObject) iterator.next();
                    String extraAppName = (String) nextJSONObject.get("name");
                    Boolean instrumented = (Boolean) nextJSONObject.get("instrumented");

                    extraAPPJSON.put("extraAppName", extraAppName);
                    extraAPPJSON.put("instrumented", instrumented ? "Packaged" : "Not Packaged");
                    if(extraApps.length() > 1){
                        extraApps.append(";\n");
                    }
                    extraApps.append(extraAppName).append("\t\t").append(instrumented ? "Packaged" : "Not Packaged");

                    returnExtraJSONArray.add(extraAPPJSON);
                }
            }
            //Test Definitions
            JSONObject returnDefinitionJSON = new JSONObject();

            JSONObject applicationJSON = (JSONObject) jobJSON.get("application");

            if (applicationJSON != null) {
                String launchApplicationName = (String) applicationJSON.get("name");
                Boolean instrumented = (Boolean) applicationJSON.get("instrumented");

                returnDefinitionJSON.put("launchApplicationName", launchApplicationName);
                returnDefinitionJSON.put("instrumented", instrumented ? "Packaged" : "Not Packaged");
            }

            //Device metrics,Install Restart
            String headerStr = (String) jobJSON.get("header");
            JSONObject headerJSON = parseJSONString(headerStr);
            if (headerJSON != null) {
                JSONObject configurationJSONObject = (JSONObject) headerJSON.get("configuration");
                Boolean restart = (Boolean) configurationJSONObject.get("restartApp");
                Boolean install = (Boolean) configurationJSONObject.get("installAppBeforeExecution");
                Boolean uninstall = (Boolean) configurationJSONObject.get("deleteAppAfterExecution");

                StringBuffer sb = new StringBuffer("");

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
                StringBuffer deviceMetricsSb = new StringBuffer("");
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
            returnJSON.put("extraApps", extraApps.toString());
            returnJSON.put("extraApps2", returnExtraJSONArray);
            returnJSON.put("definitions", returnDefinitionJSON);
            returnJSON.put("jobUUID", jobUUID);
            returnJSON.put("deviceJSON", returnDeviceJSON);
        }
        return returnJSON;
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
        if(StringUtils.isNullOrEmpty(setCookie)){
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

    private boolean argumentsCheck(String... args) {

        for (String arg : args) {
            if (arg == null || arg == "" || arg.length() == 0) {
                return false;
            }
        }
        return true;
    }

    private String removeLastSemicolon(StringBuffer sb) {
        String result = sb.toString();
        int indexOf = result.lastIndexOf(";");
        if(indexOf > 0){
            result = result.substring(0, indexOf);
        }
        return result;
    }

    private JSONObject removeIcon(JSONObject jobJSON){
        JSONArray extArr = null;
        if (jobJSON != null) {
            if (jobJSON != null) {
                JSONObject applicationJSONObject = (JSONObject) jobJSON.get("application");
                if (applicationJSONObject != null) {
                    applicationJSONObject.remove(Constants.ICON);
                }
                extArr = (JSONArray) jobJSON.get("extraApps");
                if (extArr != null) {
                    Iterator<Object> iterator = extArr.iterator();
                    while (iterator.hasNext()) {
                        JSONObject extAppJSONObject = (JSONObject) iterator.next();
                        extAppJSONObject.remove(Constants.ICON);
                    }
                }
            }
        }
        return jobJSON;
    }
}
