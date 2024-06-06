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
