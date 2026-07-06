package com.zbkj.common.utils;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.PaymentModeConstants;

/**
 * 支付模式工具
 */
public class PaymentModeUtil {

    public static Boolean isValidMode(String mode) {
        return PaymentModeConstants.MODE_OFFLINE_QR.equals(mode)
                || PaymentModeConstants.MODE_WECHAT_ONLINE.equals(mode);
    }

    public static String resolveMode(String mode, String offlineStatus, String wechatStatus) {
        if (isValidMode(mode)) {
            return mode;
        }
        if (OfflinePayUtil.isConfigOpen(offlineStatus)) {
            return PaymentModeConstants.MODE_OFFLINE_QR;
        }
        if (OfflinePayUtil.isConfigOpen(wechatStatus)) {
            return PaymentModeConstants.MODE_WECHAT_ONLINE;
        }
        return PaymentModeConstants.MODE_OFFLINE_QR;
    }

    public static Boolean isOfflineQrMode(String mode) {
        return PaymentModeConstants.MODE_OFFLINE_QR.equals(mode);
    }

    public static Boolean isWechatOnlineMode(String mode) {
        return PaymentModeConstants.MODE_WECHAT_ONLINE.equals(mode);
    }

    public static String switchValue(Boolean open) {
        return Boolean.TRUE.equals(open) ? Constants.CONFIG_FORM_SWITCH_OPEN : Constants.CONFIG_FORM_SWITCH_CLOSE;
    }

    public static String modeName(String mode) {
        if (PaymentModeConstants.MODE_WECHAT_ONLINE.equals(mode)) {
            return PaymentModeConstants.MODE_NAME_WECHAT_ONLINE;
        }
        return PaymentModeConstants.MODE_NAME_OFFLINE_QR;
    }

    public static String requireValidMode(String mode) {
        if (StrUtil.isBlank(mode) || !isValidMode(mode)) {
            throw new IllegalArgumentException("不支持的支付模式：" + mode);
        }
        return mode;
    }

    private PaymentModeUtil() {
    }
}
