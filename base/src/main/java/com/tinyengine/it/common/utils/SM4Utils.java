package com.tinyengine.it.common.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

public class SM4Utils {

    private static final String ALGORITHM = "SM4";
    private static final String TRANSFORMATION = "SM4/GCM/NoPadding";
    private static final int KEY_SIZE = 128;
    private static final int KEY_LENGTH_BYTES = KEY_SIZE / Byte.SIZE;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 生成 SM4 密钥
     */
    public static String generateKeyBase64() throws Exception {
        byte[] key = generateKey();
        return Base64.getEncoder().encodeToString(key);
    }

    public static byte[] generateKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM, "BC");
        kg.init(KEY_SIZE, SECURE_RANDOM);
        SecretKey secretKey = kg.generateKey();
        return secretKey.getEncoded();
    }

    public static String encrypt(String apiKey, String base64Key) throws Exception {
        byte[] key = decodeKey(base64Key);
        byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        byte[] encrypted = doCipher(Cipher.ENCRYPT_MODE, apiKey.getBytes(StandardCharsets.UTF_8), key, iv);
        byte[] output = ByteBuffer.allocate(iv.length + encrypted.length)
            .put(iv)
            .put(encrypted)
            .array();
        return Base64.getEncoder().encodeToString(output);
    }

    public static String decrypt(String encryptedBase64, String base64Key) throws Exception {
        byte[] key = decodeKey(base64Key);
        byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedBase64);
        if (encryptedWithIv.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid encrypted payload");
        }

        ByteBuffer buffer = ByteBuffer.wrap(encryptedWithIv);
        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        byte[] decrypted = doCipher(Cipher.DECRYPT_MODE, encrypted, key, iv);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static byte[] doCipher(int mode, byte[] data, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, "BC");
        cipher.init(mode, secretKeySpec, parameterSpec);
        return cipher.doFinal(data);
    }

    private static byte[] decodeKey(String base64Key) {
        byte[] key = Base64.getDecoder().decode(base64Key);
        if (key.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("SM4 key must be 128 bits");
        }
        return key;
    }
}
