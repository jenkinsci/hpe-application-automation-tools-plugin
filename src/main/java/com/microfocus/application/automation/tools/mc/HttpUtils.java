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

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpUtils {

    public static final String POST = "POST";
    public static final String GET = "GET";

    private HttpUtils() {

    }

    public static HttpResponse doPost(ProxyInfo proxyInfo, String url, Map<String, String> headers, byte[] data) {
        HttpResponse response = null;
        try {
            response = doHttp(proxyInfo, POST, url, null, headers, data, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public static HttpResponse doPost(ProxyInfo proxyInfo, String url, Map<String, String> headers, byte[] data, boolean useCookieManager) {
        HttpResponse response = null;
        try {
            response = doHttp(proxyInfo, POST, url, null, headers, data, useCookieManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public static HttpResponse doGet(ProxyInfo proxyInfo, String url, Map<String, String> headers, String queryString) {
        HttpResponse response = null;
        try {
            response = doHttp(proxyInfo, GET, url, queryString, headers, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    private static HttpResponse doHttp(ProxyInfo proxyInfo, String requestMethod, String connectionUrl, String queryString, Map<String, String> headers, byte[] data, boolean useCookieManager) throws IOException {
        HttpResponse response = new HttpResponse();

        if ((queryString != null) && !queryString.isEmpty()) {
            connectionUrl += "?" + queryString;
        }

        URL url = new URL(connectionUrl);
        CookieManager cookieManager = null;
        if (useCookieManager) {
            cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);
        }

        HttpURLConnection connection = (HttpURLConnection) openConnection(proxyInfo, url);

        connection.setRequestMethod(requestMethod);

        setConnectionHeaders(connection, headers);

        if (data != null && data.length > 0) {
            connection.setDoOutput(true);
            try {
                OutputStream out = connection.getOutputStream();
                out.write(data);
                out.flush();
                out.close();
            } catch (Throwable cause) {
                cause.printStackTrace();
            }
        }

        connection.connect();

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            Object object = convertStreamToObject(inputStream);
            response.setHeaders(connection.getHeaderFields());
            if (null == object) {
                System.out.println(requestMethod + " " + connectionUrl + " return is null.");
            } else if (object instanceof JSONObject) {
                response.setJsonObject((JSONObject) object);
            } else if (object instanceof JSONArray) {
                response.setJsonArray((JSONArray) object);
            } else if(object instanceof Boolean){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", !((Boolean) object).booleanValue());
                response.setJsonObject(jsonObject);
            }
            if (useCookieManager) {
                response.setCookiesString(getCookiesString(cookieManager));
            }
        } else {
            System.out.println(requestMethod + " " + connectionUrl + " failed with response code:" + responseCode);
        }
        connection.disconnect();

        return response;
    }

    private static String getCookiesString(CookieManager cookieManager) {
        StringBuilder ret = new StringBuilder();
        if (cookieManager != null) {
            List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
            for (HttpCookie cookie : cookies) {
                //TODO review each cookie !!!!!!!!!!!!!!!!!!!!
                ret.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");
            }
        }
        return ret.toString();
    }


    private static URLConnection openConnection(final ProxyInfo proxyInfo, URL _url) throws IOException {

        Proxy proxy = null;

        if (proxyInfo != null && !proxyInfo.isEmpty()) {
            try {
                int port = Integer.parseInt(proxyInfo.port.trim());
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyInfo.host, port));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if (proxy != null && !proxyInfo.isEmpty()) {
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyInfo.userName, proxyInfo.password.toCharArray());    //To change body of overridden methods use File | Settings | File Templates.
                }
            };

            Authenticator.setDefault(authenticator);
        }

        if (proxy == null) {
            return _url.openConnection();
        }


        return _url.openConnection(proxy);
    }


    private static void setConnectionHeaders(HttpURLConnection connection, Map<String, String> headers) {

        if (connection != null && headers != null && headers.size() != 0) {
            Iterator<Map.Entry<String, String>> headersIterator = headers.entrySet().iterator();
            while (headersIterator.hasNext()) {
                Map.Entry<String, String> header = headersIterator.next();
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

    }

    private static Object convertStreamToObject(InputStream inputStream) {
        Object obj = null;

        if (inputStream != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuffer res = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null) {
                    res.append(line);
                }
                obj = JSONValue.parseStrict(res.toString());
            } catch (ClassCastException e) {
                System.out.println("WARN::INVALIDE JSON Object" + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return obj;
    }


    public static ProxyInfo setProxyCfg(String host, String port, String userName, String password) {

        return new ProxyInfo(host, port, userName, password);
    }

    public static ProxyInfo setProxyCfg(String host, String port) {

        ProxyInfo proxyInfo = new ProxyInfo();

        proxyInfo.host = host;
        proxyInfo.port = port;

        return proxyInfo;
    }

    public static ProxyInfo setProxyCfg(String address, String userName, String password) {
        ProxyInfo proxyInfo = new ProxyInfo();

        if (address != null) {
            if (address.endsWith("/")) {
                int end = address.lastIndexOf("/");
                address = address.substring(0, end);
            }

            int index = address.lastIndexOf(':');
            if (index > 0) {
                proxyInfo.host = address.substring(0, index);
                proxyInfo.port = address.substring(index + 1, address.length());
            } else {
                proxyInfo.host = address;
                proxyInfo.port = "80";
            }
        }
        proxyInfo.userName = userName;
        proxyInfo.password = password;

        return proxyInfo;
    }

    static class ProxyInfo {
        String host;
        String port;
        String userName;
        String password;

        public ProxyInfo() {

        }

        public ProxyInfo(String host, String port, String userName, String password) {
            this.host = host;
            this.port = port;
            this.userName = userName;
            this.password = password;
        }

        public boolean isEmpty() {
            return StringUtils.isEmpty(host) || StringUtils.isEmpty(port) || StringUtils.isEmpty(userName) || StringUtils.isEmpty(password);
        }

    }
}