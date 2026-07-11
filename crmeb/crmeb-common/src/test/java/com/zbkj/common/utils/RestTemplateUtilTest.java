package com.zbkj.common.utils;

import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class RestTemplateUtilTest {

    private RestTemplateUtil restTemplateUtil;
    private MockRestServiceServer server;

    @Before
    public void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        restTemplateUtil = new RestTemplateUtil(restTemplate);
    }

    @Test
    public void postJsonDataSendsTheConfiguredJsonEntity() {
        JSONObject payload = new JSONObject();
        payload.put("orderId", 42);
        server.expect(requestTo("https://example.test/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"orderId\":42}"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        assertEquals("ok", restTemplateUtil.postJsonData("https://example.test/orders", payload));
        server.verify();
    }

    @Test
    public void postJsonBufferAlsoKeepsJsonHeaders() {
        JSONObject payload = new JSONObject();
        payload.put("scene", "qr");
        byte[] expected = "png".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo("https://example.test/buffer"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"scene\":\"qr\"}"))
                .andRespond(withSuccess(expected, MediaType.APPLICATION_OCTET_STREAM));

        assertArrayEquals(expected,
                restTemplateUtil.postJsonDataAndReturnBuffer("https://example.test/buffer", payload));
        server.verify();
    }
}
