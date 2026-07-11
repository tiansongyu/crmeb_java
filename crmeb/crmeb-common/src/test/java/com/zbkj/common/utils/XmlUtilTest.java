package com.zbkj.common.utils;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XmlUtilTest {

    @Test
    public void parsesSimplePaymentXml() {
        HashMap<String, Object> result = XmlUtil.xmlToMap(
                "<xml><return_code>SUCCESS</return_code><order_id>42</order_id></xml>");

        assertEquals("SUCCESS", result.get("return_code"));
        assertEquals("42", result.get("order_id"));
    }

    @Test
    public void rejectsExternalEntitiesInStringXml() {
        String malicious = "<!DOCTYPE xml [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<xml><content>&xxe;</content></xml>";

        Map<String, Object> result = XmlUtil.xmlToMap(malicious);

        assertTrue(result.isEmpty());
        assertFalse(result.containsKey("content"));
    }

    @Test
    public void rejectsExternalEntitiesInRequestXml() {
        String malicious = "<!DOCTYPE xml [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<xml><content>&xxe;</content></xml>";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(malicious.getBytes(StandardCharsets.UTF_8));

        Map<String, String> result = XmlUtil.xmlToMap(request);

        assertTrue(result.isEmpty());
    }
}
