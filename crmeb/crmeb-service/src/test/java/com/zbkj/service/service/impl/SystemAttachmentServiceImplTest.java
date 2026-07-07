package com.zbkj.service.service.impl;

import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.service.service.SystemConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class SystemAttachmentServiceImplTest {

    private SystemAttachmentServiceImpl service;

    @Mock
    private SystemConfigService systemConfigService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new SystemAttachmentServiceImpl();
        ReflectionTestUtils.setField(service, "systemConfigService", systemConfigService);
        when(systemConfigService.getValueByKeyException(SysConfigConstants.CONFIG_UPLOAD_TYPE)).thenReturn("1");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_LOCAL_UPLOAD_URL)).thenReturn("https://cdn.example.com");
    }

    @Test
    public void prefixImageNormalizesLocalAndUndefinedMediaPaths() {
        assertEquals(
                "https://cdn.example.com/crmebimage/public/order/proof.png",
                service.prefixImage("crmebimage/public/order/proof.png"));
        assertEquals(
                "https://cdn.example.com/crmebimage/public/order/proof.png",
                service.prefixImage("undefined/crmebimage/public/order/proof.png"));
    }

    @Test
    public void prefixImageStripsPrivateMediaHosts() {
        assertEquals(
                "https://cdn.example.com/crmebimage/public/order/proof.png",
                service.prefixImage("http://192.168.31.35:20400/crmebimage/public/order/proof.png"));
        assertEquals(
                "https://cdn.example.com/crmebimage/public/order/proof.png",
                service.prefixImage("//10.0.0.8:20400/crmebimage/public/order/proof.png"));
        assertEquals(
                "https://cdn.example.com/crmebimage/public/order/proof.png",
                service.prefixUploadf("//172.20.0.8:20400/crmebimage/public/order/proof.png"));
    }

    @Test
    public void prefixImageKeepsPublicMediaUrlsAndBlankValues() {
        assertEquals(
                "https://oss.example.com/crmebimage/public/order/proof.png",
                service.prefixImage("https://oss.example.com/crmebimage/public/order/proof.png"));
        assertEquals(
                "//cdn.example.com/crmebimage/public/order/proof.png",
                service.prefixImage("//cdn.example.com/crmebimage/public/order/proof.png"));
        assertEquals("", service.prefixImage(""));
        assertEquals(null, service.prefixImage(null));
    }
}
