package com.zbkj.service.service.impl;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.OfflinePayConstants;
import com.zbkj.common.constants.PaymentModeConstants;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.request.PaymentModeSwitchRequest;
import com.zbkj.common.response.PaymentModeResponse;
import com.zbkj.service.service.SystemConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PaymentModeServiceImplTest {

    private PaymentModeServiceImpl service;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new PaymentModeServiceImpl();
        ReflectionTestUtils.setField(service, "systemConfigService", systemConfigService);
        ReflectionTestUtils.setField(service, "transactionTemplate", transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Boolean> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    public void getsModeFromLegacyOfflineConfigWhenPayModeIsMissing() {
        when(systemConfigService.getValueByKey(PaymentModeConstants.CONFIG_PAY_MODE)).thenReturn("");
        when(systemConfigService.getValueByKey(OfflinePayConstants.CONFIG_OFFLINE_PAY_STATUS)).thenReturn("1");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_WEIXIN_OPEN)).thenReturn(Constants.CONFIG_FORM_SWITCH_OPEN);

        PaymentModeResponse response = service.getMode();

        assertEquals(PaymentModeConstants.MODE_OFFLINE_QR, response.getMode());
        assertTrue(response.getOfflinePayOpen());
        assertFalse(response.getWechatPayOpen());
    }

    @Test
    public void switchingToOfflineQrWritesMutuallyExclusiveLegacyConfig() {
        when(systemConfigService.getValueByKey(OfflinePayConstants.CONFIG_OFFLINE_PAY_QRCODE)).thenReturn("crmebimage/qrcode.png");

        PaymentModeSwitchRequest request = new PaymentModeSwitchRequest();
        request.setMode(PaymentModeConstants.MODE_OFFLINE_QR);

        PaymentModeResponse response = service.switchMode(request);

        assertEquals(PaymentModeConstants.MODE_OFFLINE_QR, response.getMode());
        verify(systemConfigService).updateOrSaveValueByName(PaymentModeConstants.CONFIG_PAY_MODE, PaymentModeConstants.MODE_OFFLINE_QR);
        verify(systemConfigService).updateOrSaveValueByName(OfflinePayConstants.CONFIG_OFFLINE_PAY_STATUS, Constants.CONFIG_FORM_SWITCH_OPEN);
        verify(systemConfigService).updateOrSaveValueByName(SysConfigConstants.CONFIG_PAY_WEIXIN_OPEN, Constants.CONFIG_FORM_SWITCH_CLOSE);
    }

    @Test(expected = CrmebException.class)
    public void switchingToOfflineQrRequiresQrCode() {
        when(systemConfigService.getValueByKey(OfflinePayConstants.CONFIG_OFFLINE_PAY_QRCODE)).thenReturn("");

        PaymentModeSwitchRequest request = new PaymentModeSwitchRequest();
        request.setMode(PaymentModeConstants.MODE_OFFLINE_QR);

        service.switchMode(request);
    }

    @Test
    public void switchingToWechatOnlineWritesMutuallyExclusiveLegacyConfig() {
        when(systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID)).thenReturn("appid");
        when(systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID)).thenReturn("mchid");
        when(systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY)).thenReturn("key");

        PaymentModeSwitchRequest request = new PaymentModeSwitchRequest();
        request.setMode(PaymentModeConstants.MODE_WECHAT_ONLINE);

        PaymentModeResponse response = service.switchMode(request);

        assertEquals(PaymentModeConstants.MODE_WECHAT_ONLINE, response.getMode());
        verify(systemConfigService).updateOrSaveValueByName(PaymentModeConstants.CONFIG_PAY_MODE, PaymentModeConstants.MODE_WECHAT_ONLINE);
        verify(systemConfigService).updateOrSaveValueByName(OfflinePayConstants.CONFIG_OFFLINE_PAY_STATUS, Constants.CONFIG_FORM_SWITCH_CLOSE);
        verify(systemConfigService).updateOrSaveValueByName(SysConfigConstants.CONFIG_PAY_WEIXIN_OPEN, Constants.CONFIG_FORM_SWITCH_OPEN);
    }

    @Test(expected = CrmebException.class)
    public void switchingToWechatOnlineRequiresWechatCredentials() {
        when(systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID)).thenReturn("");
        when(systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_ROUTINE_APP_ID)).thenReturn("");

        PaymentModeSwitchRequest request = new PaymentModeSwitchRequest();
        request.setMode(PaymentModeConstants.MODE_WECHAT_ONLINE);

        service.switchMode(request);
    }
}
