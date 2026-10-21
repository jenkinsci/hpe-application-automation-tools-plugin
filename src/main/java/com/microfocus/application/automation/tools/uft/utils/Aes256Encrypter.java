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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class Aes256Encrypter {

    // =========================================================
    // ATTRIBUTES
    // =========================================================

    private static final SecureRandom secureRandom = new SecureRandom();

    private volatile SecretKeySpec aesKeySpec;
    private volatile SecretKeySpec hmacKeySpec;
    private volatile byte[] secureKey;
    private volatile boolean destroyed;

    public Aes256Encrypter() {
    }

    private synchronized void initializeKey() {
        if (destroyed) {
            throw new IllegalStateException("Encrypter has been destroyed.");
        }

        if (secureKey != null) {
            return;
        }

        byte[] generatedKey = new byte[64];
        secureRandom.nextBytes(generatedKey);
        this.aesKeySpec = new SecretKeySpec(Arrays.copyOfRange(generatedKey, 0, 32), "AES");
        this.hmacKeySpec = new SecretKeySpec(Arrays.copyOfRange(generatedKey, 32, 64), "HmacSHA256");
        this.secureKey = generatedKey;
    }

    // =========================================================
    // PUBLIC API
    // =========================================================

    public boolean hasSecureKey() {
        return !destroyed && secureKey != null;
    }

    public InputStream openSecureKeyStream() {
        if (!hasSecureKey()) {
            throw new IllegalStateException("No secure key has been generated.");
        }
        return new ByteArrayInputStream(Base64.getEncoder().encode(secureKey));
    }

    public synchronized void destroy() {
        if (secureKey != null) {
            Arrays.fill(secureKey, (byte) 0);
            secureKey = null;
        }
        aesKeySpec = null;
        hmacKeySpec = null;
        destroyed = true;
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("plainText cannot be null");
        }

        initializeKey();
        return encryptSecure(plainText);
    }
    // =========================================================
    // AES-256-CBC + HMAC-SHA256
    // =========================================================

    private String encryptSecure(String plainText) {
        try {
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, new IvParameterSpec(iv));

            byte[] plaintextBytes = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // [IV | CipherText]
            byte[] data = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, data, 0, iv.length);
            System.arraycopy(ciphertext, 0, data, iv.length, ciphertext.length);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKeySpec);
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