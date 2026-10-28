package com.smart.hgnu;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encoding required by the existing campus server, kept separate from HTTP and JSON handling.
 * AES/ECB and RSA PKCS#1 v1.5 are compatibility requirements of HGSF 3.2.0, not new protocol
 * choices.
 */
final class ProtocolCrypto {
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private ProtocolCrypto() {}

    static String encryptAes(String plaintext, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP);
    }

    static String decryptAes(String base64Ciphertext, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] plaintext = cipher.doFinal(Base64.decode(base64Ciphertext, Base64.DEFAULT));
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    static String encryptKeyExchange(String plaintext, String base64PublicKey) throws Exception {
        byte[] publicKey = Base64.decode(base64PublicKey, Base64.DEFAULT);
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(
                Cipher.ENCRYPT_MODE,
                KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey)));
        // The JSON containing one 128-bit key fits in the original 1024-bit RSA block (117 bytes).
        return Base64.encodeToString(
                cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    }
}
