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
 *  Copyright 2012-2026 Open Text.
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
package com.microfocus.application.automation.tools.uft.utils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class Aes256Encrypter {

    // =========================================================
    // ATTRIBUTES
    // =========================================================

    private static Aes256Encrypter instance;

    private final byte[] aesKey;
    private final byte[] hmacKey;
    private final byte[] privateKey;

    public static byte[] getPrivateKey() {
        return instance == null ? null : instance.privateKey.clone();
    }

    // =========================================================
    // SINGLETON FACTORY
    // =========================================================

    public static synchronized void create(byte[] privateKey) {
        if (instance != null) {
            throw new IllegalStateException("Encrypter is already initialized.");
        }

        if (privateKey == null || privateKey.length == 0) {
            privateKey = new byte[64];
            new SecureRandom().nextBytes(privateKey);
        }

        instance = new Aes256Encrypter(privateKey);
    }

    private Aes256Encrypter(byte[] privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("privateKey cannot be null");
        }

        if (privateKey.length != 64) {
            throw new IllegalArgumentException(
                    "Invalid secure key length. Expected 64 bytes.");
        }

        this.privateKey = privateKey.clone();
        this.aesKey = Arrays.copyOfRange(privateKey, 0, 32);
        this.hmacKey = Arrays.copyOfRange(privateKey, 32, 64);
    }

    // =========================================================
    // PUBLIC API
    // =========================================================

    public static String encrypt(String plainText) {
        if (instance == null) {
            create(null); // lazy initialization
        }

        return instance.encryptSecure(plainText);
    }

    // =========================================================
    // AES-256-CBC + HMAC-SHA256
    // =========================================================

    private String encryptSecure(String plainText) {
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new IvParameterSpec(iv));

            byte[] plaintextBytes =
                    plainText.getBytes(StandardCharsets.UTF_8);

            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // [IV | CipherText]
            byte[] data = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, data, 0, iv.length);
            System.arraycopy(ciphertext, 0, data, iv.length,
                    ciphertext.length);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            byte[] hmac = mac.doFinal(data);

            // [IV | CipherText | HMAC]
            byte[] result = new byte[data.length + hmac.length];
            System.arraycopy(data, 0, result, 0, data.length);
            System.arraycopy(hmac, 0, result, data.length, hmac.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

}