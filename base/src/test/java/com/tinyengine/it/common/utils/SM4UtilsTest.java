package com.tinyengine.it.common.utils;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SM4UtilsTest {

    @Test
    void encryptAndDecryptRoundTrip() throws Exception {
        String key = SM4Utils.generateKeyBase64();
        String encrypted = SM4Utils.encrypt("secret-api-key", key);

        assertNotEquals("secret-api-key", encrypted);
        assertEquals("secret-api-key", SM4Utils.decrypt(encrypted, key));
    }

    @Test
    void encryptUsesRandomIv() throws Exception {
        String key = SM4Utils.generateKeyBase64();

        String first = SM4Utils.encrypt("same-plain-text", key);
        String second = SM4Utils.encrypt("same-plain-text", key);

        assertNotEquals(first, second);
    }

    @Test
    void rejectsInvalidKeyLength() {
        String invalidKey = Base64.getEncoder().encodeToString(new byte[8]);

        assertThrows(IllegalArgumentException.class, () -> SM4Utils.encrypt("secret", invalidKey));
    }
}
