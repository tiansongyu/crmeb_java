package com.zbkj.common.utils;

import com.zbkj.common.constants.PaymentModeConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PaymentModeUtilTest {

    @Test
    public void acceptsOnlySupportedModes() {
        assertTrue(PaymentModeUtil.isValidMode(PaymentModeConstants.MODE_OFFLINE_QR));
        assertTrue(PaymentModeUtil.isValidMode(PaymentModeConstants.MODE_WECHAT_ONLINE));
        assertFalse(PaymentModeUtil.isValidMode(""));
        assertFalse(PaymentModeUtil.isValidMode("both"));
    }

    @Test
    public void explicitModeWinsWhenValid() {
        assertEquals(PaymentModeConstants.MODE_WECHAT_ONLINE,
                PaymentModeUtil.resolveMode(PaymentModeConstants.MODE_WECHAT_ONLINE, "1", "0"));
        assertEquals(PaymentModeConstants.MODE_OFFLINE_QR,
                PaymentModeUtil.resolveMode(PaymentModeConstants.MODE_OFFLINE_QR, "0", "1"));
    }

    @Test
    public void legacyOfflineStatusWinsWhenPayModeIsMissing() {
        assertEquals(PaymentModeConstants.MODE_OFFLINE_QR, PaymentModeUtil.resolveMode("", "1", "'1'"));
        assertEquals(PaymentModeConstants.MODE_OFFLINE_QR, PaymentModeUtil.resolveMode(null, "'1'", "0"));
    }

    @Test
    public void legacyWechatStatusIsUsedWhenOfflineIsClosed() {
        assertEquals(PaymentModeConstants.MODE_WECHAT_ONLINE, PaymentModeUtil.resolveMode("", "0", "'1'"));
    }

    @Test
    public void blankLegacyConfigDefaultsToOfflineQr() {
        assertEquals(PaymentModeConstants.MODE_OFFLINE_QR, PaymentModeUtil.resolveMode("", "", ""));
    }

    @Test
    public void switchValuesUseExistingConfigSwitchFormat() {
        assertEquals("'1'", PaymentModeUtil.switchValue(true));
        assertEquals("'0'", PaymentModeUtil.switchValue(false));
    }
}
