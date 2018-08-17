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

package com.microfocus.application.automation.tools.sse.sdk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.microfocus.application.automation.tools.common.SSEException;

public class HttpRequestDecorator {
    
    /**
     * 
     * @param headers
     *            headrs to decorate with user info depending on the resource access level.
     * @param userName
     * @param resourceAccessLevel
     */
    public static void decorateHeaderWithUserInfo(
            final Map<String, String> headers,
            String userName,
            ResourceAccessLevel resourceAccessLevel) {
        
        if (headers == null) {
            throw new IllegalArgumentException("header must not be null");
        }
        //attach encrypted user name for protected and public resources
        if (resourceAccessLevel.equals(ResourceAccessLevel.PROTECTED)
            || resourceAccessLevel.equals(ResourceAccessLevel.PRIVATE)) {
            String userHeaderName = resourceAccessLevel.getUserHeaderName();
            String encryptedUserName = getDigestString("MD5", userName);
            if (userHeaderName != null) {
                headers.put(userHeaderName, encryptedUserName);
            }
        }
    }
    
    private static String getDigestString(String algorithmName, String dataToDigest) {
        
        try {
            MessageDigest md = MessageDigest.getInstance(algorithmName);
            byte[] digested = md.digest(dataToDigest.getBytes());
            
            return digestToString(digested);
        } catch (NoSuchAlgorithmException ex) {
            throw new SSEException(ex);
        }
    }
    
    /**
     * This method convert byte array to string regardless the charset
     * 
     * @param b
     *            byte array input
     * @return the corresponding string
     */
    private static String digestToString(byte[] b) {
        
        StringBuilder result = new StringBuilder(128);
        for (byte aB : b) {
            result.append(Integer.toString((aB & 0xff) + 0x100, 16).substring(1));
        }
        
        return result.toString();
    }
    
}
