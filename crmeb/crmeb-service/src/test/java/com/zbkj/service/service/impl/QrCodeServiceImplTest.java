package com.zbkj.service.service.impl;

import com.zbkj.common.exception.CrmebException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

public class QrCodeServiceImplTest {

    private QrCodeServiceImpl service;

    @Before
    public void setUp() {
        service = new QrCodeServiceImpl();
    }

    @Test(expected = CrmebException.class)
    public void rejectsLoopbackImageUrls() {
        service.base64("http://127.0.0.1/private.png");
    }

    @Test(expected = CrmebException.class)
    public void rejectsNonHttpImageUrls() {
        service.base64("file:///etc/passwd");
    }

    @Test(expected = CrmebException.class)
    public void rejectsOversizedQrCodeDimensions() {
        service.base64String("hello", 501, 100);
    }

    @Test
    public void createsQrCodeWithinSafeBounds() {
        Map<String, Object> result = service.base64String("hello", 100, 100);

        assertTrue(result.get("code").toString().startsWith("data:image/"));
    }
}
