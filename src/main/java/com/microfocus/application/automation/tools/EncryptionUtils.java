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
package com.microfocus.application.automation.tools;

import com.microfocus.application.automation.tools.common.Pair;
import com.microfocus.application.automation.tools.nodes.EncryptionNodeProperty;
import com.microfocus.application.automation.tools.settings.UFTEncryptionGlobalConfiguration;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import hudson.FilePath;
import hudson.model.Node;
import hudson.util.Secret;
import org.apache.commons.io.IOUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public final class EncryptionUtils {

    private static final int KEY_SIZE = 3072; // should be secure for some time
    private static final String ENC_TYPE_FOR_PROPS = "RSA";
    private static final String ENC_TYPE_FOR_NODE = "AES/CBC/PKCS7Padding";
    private static final String PRIVATE_SPEC_FOR_NODE = "AES";
    private static final String KEY_PATH = "secrets/.hptoolslaunchersecret.key";
    private static final String NL = System.getProperty("line.separator");

    private EncryptionUtils() {
        // no meaning instantiating
    }

    /**
     * Parses a public key returns its PublicKey instance.
     * @param publicKeyStr public key in base64 string format
     * @return PublicKey instance
     * @throws EncryptionException if an error occurs while recreating the PublicKey
     */
    private static PublicKey tryParsePublicKey(String publicKeyStr) throws EncryptionException {
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(ENC_TYPE_FOR_PROPS);
        } catch (NoSuchAlgorithmException ignored) {
            throw new EncryptionException("Failed to get key factory.");
        }

        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyStr.replace("\n", "").getBytes(StandardCharsets.UTF_8)));

        PublicKey publicKey;
        try {
            publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException ignored) {
            throw new EncryptionException("Failed to regenerate public key.");
        }

        return publicKey;
    }

    /**
     * Generates a new random public-private key pair.
     * @return Public and Private Keys
     * @throws EncryptionException if an error occurs while creating the keys
     */
    private static Pair<PublicKey, PrivateKey> generatePair() throws EncryptionException {
        KeyPairGenerator generator;

        try {
            generator = KeyPairGenerator.getInstance(ENC_TYPE_FOR_PROPS);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to get key pair generator.");
        }

        generator.initialize(KEY_SIZE);
        KeyPair pair = generator.generateKeyPair();
        return new Pair<>(pair.getPublic(), pair.getPrivate());
    }

    /**
     * Convert an array of bytes into base64 string format.
     * @param bytes
     * @return string
     */
    private static String getBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String getPublicKeyEncoded(PublicKey publicKey) {
        return getBase64(publicKey.getEncoded());
    }

    /**
     * Convert a BigInteger instance into an array of bytes, takes into account the sign bit as well.
     * @param bigInt
     * @return
     */
    static byte[] getBytes(BigInteger bigInt) {
        byte[] bytes = bigInt.toByteArray();
        int length = bytes.length;

        if (length % 2 != 0 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, length);
        }

        return bytes;
    }

    /**
     * Used in XML format conversion, creates a new string formatted XML node.
     * @param name
     * @param bigInt
     * @return
     */
    private static String getElement(String name, BigInteger bigInt) {
        String cnt = getBase64(getBytes(bigInt));
        return String.format("<%s>%s</%s>%s", name, cnt, name, NL);
    }

    /**
     * Converts a Private Key into XML format used in C#, needed when we want to save the private key for the hptoolslauncher, currently a limitation.
     * @param key
     * @return
     * @throws EncryptionException if an error occurs while the conversion
     */
    private static String convertPrivateKeyToXMLFormat(PrivateKey key) throws EncryptionException {
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(ENC_TYPE_FOR_PROPS);
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to get key factory.");
        }

        RSAPrivateCrtKeySpec spec;
        try {
            spec = keyFactory.getKeySpec(key, RSAPrivateCrtKeySpec.class);
        } catch (InvalidKeySpecException e) {
            throw new EncryptionException("Failed to create specification for private key.");
        }

        return "<RSAKeyValue>" + NL +
                getElement("Modulus", spec.getModulus()) +
                getElement("Exponent", spec.getPublicExponent()) +
                getElement("P", spec.getPrimeP()) +
                getElement("Q", spec.getPrimeQ()) +
                getElement("DP", spec.getPrimeExponentP()) +
                getElement("DQ", spec.getPrimeExponentQ()) +
                getElement("InverseQ", spec.getCrtCoefficient()) +
                getElement("D", spec.getPrivateExponent()) +
                "</RSAKeyValue>";
    }

    /**
     * Saves the private key on the executor node.
     * @param root
     * @param key
     * @throws EncryptionException
     */
    private static void savePrivateKeyForNode(FilePath root, PrivateKey key) throws EncryptionException {
        // we have a private key, needs to be saved to the agent's fs encrypted
        Secret sk;
        try {
            sk = Secret.fromString(UFTEncryptionGlobalConfiguration.getInstance().getEncKey());
        } catch (NullPointerException ignored) {
            throw new EncryptionException("Jenkins cannot find the module for encryption secret key.");
        }

        String privateKey = convertPrivateKeyToXMLFormat(key);
        String encrypted = encryptWithPwd(privateKey, sk.getPlainText());
        InputStream encryptedAsStream = IOUtils.toInputStream(encrypted, StandardCharsets.UTF_8);

        try {
            root.child(KEY_PATH).copyFrom(encryptedAsStream);
        } catch (IOException | InterruptedException e) {
            throw new EncryptionException("Failed to save private key to executor node: " + e.getMessage() + ".");
        }
    }

    /**
     * Encrypts the data using the node's public key.
     * @param text to be encrypted
     * @param currNode current executor
     * @return encrypted string in base64 format
     * @throws EncryptionException
     */
    public static String encrypt(String text, Node currNode) throws EncryptionException {
        EncryptionNodeProperty publicKeyProp = currNode.getNodeProperty(EncryptionNodeProperty.class);

        if (publicKeyProp == null) {
            // if the encryption node property is not enabled for this node, enable it automatically
            currNode.getNodeProperties().add(new EncryptionNodeProperty());
            publicKeyProp = currNode.getNodeProperty(EncryptionNodeProperty.class);
        }

        if (publicKeyProp == null) throw new EncryptionException("You need to enable encryption in Node configuration manually first, automatic addition failed before running UFT tests.");

        String publicKeyStr = Secret.fromString(publicKeyProp.getPublicKey()).getPlainText();
        PublicKey publicKey;

        if (StringUtils.isNullOrEmpty(publicKeyStr)) {
            Pair<PublicKey, PrivateKey> encPair;
            try {
                // we generate a new pair for this node
                encPair = generatePair();
            } catch (EncryptionException ignored) {
                throw new EncryptionException("Failed to generate key pairs for encryption.");
            }

            publicKey = encPair.getFirst();
            publicKeyStr = getPublicKeyEncoded(publicKey);

            // save the private to the node, encrypted, we will provide during runtime the decryption key
            savePrivateKeyForNode(currNode.getRootPath(), encPair.getSecond());

            // save the public on the server
            publicKeyProp.setPublicKey(publicKeyStr);

            try {
                currNode.save();
            } catch (IOException e) {
                throw new EncryptionException("Failed to save public key on executor node: " + e.getMessage() + ".");
            }
        } else {
            publicKey = tryParsePublicKey(publicKeyStr);
        }

        return encrypt(text, publicKey);
    }

    /**
     * Encrypts the data with the given PublicKey.
     * @param text to be encrypted
     * @param publicKey
     * @return encrypted string in base64 format
     * @throws EncryptionException
     */
    public static String encrypt(String text, PublicKey publicKey) throws EncryptionException {
        Cipher encryptCipher;
        try {
            encryptCipher = Cipher.getInstance(ENC_TYPE_FOR_PROPS);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ignored) {
            throw new EncryptionException("Failed to obtain " + ENC_TYPE_FOR_PROPS + " cipher.");
        }

        try {
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (InvalidKeyException ignored) {
            throw new EncryptionException("Failed to initialize " + ENC_TYPE_FOR_PROPS + " cipher.");
        }

        byte[] plainBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes;
        try {
            encryptedBytes = encryptCipher.doFinal(plainBytes);
        } catch (IllegalBlockSizeException | BadPaddingException ignored) {
            throw new EncryptionException("Failed to encrypt data.");
        }

        return getBase64(encryptedBytes);
    }

    /**
     * Internal usage only, used when saving private key on executor node.
     * @param text
     * @param pwd
     * @return
     * @throws EncryptionException
     */
    private static String encryptWithPwd(String text, String pwd) throws EncryptionException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(ENC_TYPE_FOR_NODE);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ignored) {
            throw new EncryptionException("Failed to obtain " + ENC_TYPE_FOR_NODE + " cipher.");
        }

        SecretKeySpec keySpec = new SecretKeySpec(pwd.getBytes(StandardCharsets.UTF_8), PRIVATE_SPEC_FOR_NODE);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new SecureRandom());
        } catch (InvalidKeyException ignored) {
            throw new EncryptionException("Failed to initialize " + ENC_TYPE_FOR_NODE + " cipher.");
        }

        byte[] encrypted;
        try {
            encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalBlockSizeException | BadPaddingException ignored) {
            throw new EncryptionException("Failed to encrypt data.");
        }

        byte[] ivBytes = cipher.getIV();
        byte[] combined = new byte[ivBytes.length + encrypted.length];
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
        System.arraycopy(encrypted, 0, combined, ivBytes.length, encrypted.length);

        return getBase64(combined);
    }

    public static class EncryptionException extends Exception {
        public EncryptionException(String message) {
            super(message);
        }

        @Override
        public String getMessage() {
            return super.getMessage();
        }
    }
}
