/**
 *
 */
package qc.rest.examples.infrastructure;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
/**
 * This class keeps the state of the connection for the examples. This class is a
 * singleton, and all examples get the instance in their default constructors - thus sharing state
 * (cookies, server url).
 *
 * Some simple methods are implemented to get commonly used paths.
 *
 */
public class RestConnector {
    protected Map<String, String> cookies;
    protected String host;
    //protected String port;
    protected String domain;
    protected String project;
    public RestConnector init(
            Map<String, String> cookies,
            String host,
            /*String port,*/
            String domain,
            String project) {
        this.cookies = cookies;
        this.host = host;
        //this.port = port;
        this.domain = domain;
        this.project = project;
        return this;
    }
    private RestConnector() {}
    private static RestConnector instance = new RestConnector();
    public static RestConnector getInstance() {
        return instance;
    }
    /**
     * @param entityType
     * @return
     */
    public String buildEntityCollectionUrl(String entityType) {
        return buildUrl("rest/domains/"
                        + domain
                        + "/projects/"
                        + project
                        + "/"
                        + entityType);
    }
    /**
     * @param path
     *            on the server to use
     * @return a url on the server for the path parameter
     */
    public String buildUrl(String path) {
        //return String.format("http://%1$s:%2$s/%3$s", host, port, path);
    	return String.format("%1$s/%2$s", host, path);
    }
    /**
     * @return the cookies
     */
    public Map<String, String> getCookies() {
        return cookies;
    }
    /**
     * @param cookies
     *            the cookies to set
     */
    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }
    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }
    /**
     * @param host
     *            the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }
    /**
     * @return the port
     */
    /*public String getPort() {
        return port;
    }*/
    /**
     * @param port
     *            the port to set
     */
    /*public void setPort(String port) {
        this.port = port;
    }*/
    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }
    /**
     * @param domain
     *            the domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }
    /**
     * @return the project
     */
    public String getProject() {
        return project;
    }
    /**
     * @param project
     *            the project to set
     */
    public void setProject(String project) {
        this.project = project;
    }
    public Response httpPut(String url, byte[] data, Map<String, String> headers) throws Exception {
        return doHttp("PUT", url, null, data, headers, cookies);
    }
    public Response httpPost(String url, byte[] data, Map<String, String> headers) throws Exception {
        return doHttp("POST", url, null, data, headers, cookies);
    }
    public Response httpDelete(String url, Map<String, String> headers) throws Exception {
        return doHttp("DELETE", url, null, null, headers, cookies);
    }
    public Response httpGet(String url, String queryString, Map<String, String> headers)
            throws Exception {
        return doHttp("GET", url, queryString, null, headers, cookies);
    }
    /**
     * @param type
     *            of the http operation: get post put delete
     * @param url
     *            to work on
     * @param queryString
     * @param data
     *            to write, if a writable operation
     * @param headers
     *            to use in the request
     * @param cookies
     *            to use in the request and update from the response
     * @return http response
     * @throws Exception
     */
    private Response doHttp(
            String type,
            String url,
            String queryString,
            byte[] data,
            Map<String, String> headers,
            Map<String, String> cookies) throws Exception {
        if ((queryString != null) && !queryString.isEmpty()) {
            url += "?" + queryString;
        }
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod(type);
        String cookieString = getCookieString();
        prepareHttpRequest(con, headers, data, cookieString);
        con.connect();
        Response ret = retrieveHtmlResponse(con);
        updateCookies(ret);
        return ret;
    }
    /**
     * @param con
     *            to set the headers and bytes in
     * @param headers
     *            to use in the request, such as content-type
     * @param bytes
     *            the actual data to post in the connection.
     * @param cookiestring
     *            the cookies data from clientside, such as lwsso, qcsession, jsession etc..
     * @throws IOException
     */
    private void prepareHttpRequest(
            HttpURLConnection con,
            Map<String, String> headers,
            byte[] bytes,
            String cookieString) throws IOException {
        String contentType = null;
        //Attach cookie information if it exists.
        if ((cookieString != null) && !cookieString.isEmpty()) {
            con.setRequestProperty("Cookie", cookieString);
        }
        //Send data from headers.
        if (headers != null) {
            //Skip the content-type header. The content-type header should only be sent if you are sending content. See below.
            contentType = headers.remove("Content-Type");
            Iterator<Entry<String, String>> headersIterator = headers.entrySet().iterator();
            while (headersIterator.hasNext()) {
                Entry<String, String> header = headersIterator.next();
                con.setRequestProperty(header.getKey(), header.getValue());
            }
        }
        //If there is data to attach to the request, it's handled here. Note that if data exists, we take into account previously removed content-type.
        if ((bytes != null) && (bytes.length > 0)) {
            con.setDoOutput(true);
            //Warning: If you add a content-type header then it is an error not to send information.
            if (contentType != null) {
                con.setRequestProperty("Content-Type", contentType);
            }
            OutputStream out = con.getOutputStream();
            out.write(bytes);
            out.flush();
            out.close();
        }
    }
    /**
     * @param con
     *            that already connected to it's url with an http request, and that should contain a
     *            response for us to retrieve
     * @return a response from the server to the previously submitted http request
     * @throws Exception
     */
    private Response retrieveHtmlResponse(HttpURLConnection con) throws Exception {
        Response ret = new Response();
        ret.setStatusCode(con.getResponseCode());
        ret.setResponseHeaders(con.getHeaderFields());
        InputStream inputStream;
        //Select the source of the input bytes, first try "regular" input
        try {
            inputStream = con.getInputStream();
        }
        /*
         If the connection to the server failed, for example 404 or 500, con.getInputStream() throws an exception, which is saved.
The body of the exception page is stored in the response data.
         */
        catch (Exception e) {
            inputStream = con.getErrorStream();
            ret.setFailure(e);
        }
        //This takes the data from the previously decided stream (error or input) and stores it in a byte[] inside the response.
        ByteArrayOutputStream container = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read;
        while ((read = inputStream.read(buf, 0, 1024)) > 0) {
            container.write(buf, 0, read);
        }
        ret.setResponseData(container.toByteArray());
        return ret;
    }
    private void updateCookies(Response response) {
        Iterable<String> newCookies = response.getResponseHeaders().get("Set-Cookie");
        if (newCookies != null) {
            for (String cookie : newCookies) {
                int equalIndex = cookie.indexOf('=');
                int semicolonIndex = cookie.indexOf(';');
                String cookieKey = cookie.substring(0, equalIndex);
                String cookieValue = cookie.substring(equalIndex + 1, semicolonIndex);
                cookies.put(cookieKey, cookieValue);
            }
        }
    }
    public String getCookieString() {
        StringBuilder sb = new StringBuilder();
        if (!cookies.isEmpty()) {
            Set<Entry<String, String>> cookieEntries = cookies.entrySet();
            for (Entry<String, String> entry : cookieEntries) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
        }
        String ret = sb.toString();
        return ret;
    }
}
