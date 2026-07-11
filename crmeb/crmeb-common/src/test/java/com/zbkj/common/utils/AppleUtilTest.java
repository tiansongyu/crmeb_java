package com.zbkj.common.utils;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class AppleUtilTest {

    @Test
    public void parsesBase64UrlIdentityTokenPayload() {
        String header = encode("{\"alg\":\"RS256\",\"kid\":\"key-1\"}");
        String payload = encode("{\"aud\":\"com.example.app\",\"sub\":\"user-1\"}");

        JSONObject claims = AppleUtil.parserIdentityToken(header + "." + payload + ".signature");

        assertEquals("com.example.app", claims.getString("aud"));
        assertEquals("user-1", claims.getString("sub"));
    }

    @Test
    public void malformedTokensAreRejectedWithoutKeyLookup() {
        assertNull(AppleUtil.parserIdentityToken("invalid"));
        assertFalse(AppleUtil.verify("invalid"));
    }

    private String encode(String value) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
