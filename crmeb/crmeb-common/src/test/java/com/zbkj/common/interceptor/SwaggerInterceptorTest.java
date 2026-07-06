package com.zbkj.common.interceptor;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwaggerInterceptorTest {

    @Test
    public void acceptsMatchingBasicAuthCredentials() throws Exception {
        SwaggerInterceptor interceptor = new SwaggerInterceptor("admin", "secret", true);

        assertTrue(interceptor.httpBasicAuth(basicAuth("admin", "secret")));
    }

    @Test
    public void rejectsMismatchedBasicAuthCredentials() throws Exception {
        SwaggerInterceptor interceptor = new SwaggerInterceptor("admin", "secret", true);

        assertFalse(interceptor.httpBasicAuth(basicAuth("admin", "wrong")));
    }

    @Test
    public void allowsAccessWhenSwaggerAuthCheckIsDisabled() throws Exception {
        SwaggerInterceptor interceptor = new SwaggerInterceptor("admin", "secret", false);

        assertTrue(interceptor.httpBasicAuth(null));
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
