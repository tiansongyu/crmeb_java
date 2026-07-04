package com.zbkj.common.utils;

import com.zbkj.common.constants.OfflinePayConstants;

/**
 * 线下扫码转账支付工具
 */
public class OfflinePayUtil {

    public static String getStatusText(Integer status) {
        if (OfflinePayConstants.STATUS_PENDING.equals(status)) {
            return OfflinePayConstants.STATUS_TEXT_PENDING;
        }
        if (OfflinePayConstants.STATUS_APPROVED.equals(status)) {
            return OfflinePayConstants.STATUS_TEXT_APPROVED;
        }
        if (OfflinePayConstants.STATUS_REJECTED.equals(status)) {
            return OfflinePayConstants.STATUS_TEXT_REJECTED;
        }
        return OfflinePayConstants.STATUS_TEXT_NOT_SUBMITTED;
    }

    public static Boolean isPending(Integer status) {
        return OfflinePayConstants.STATUS_PENDING.equals(status);
    }

    public static Boolean isRejected(Integer status) {
        return OfflinePayConstants.STATUS_REJECTED.equals(status);
    }

    public static Boolean isApproved(Integer status) {
        return OfflinePayConstants.STATUS_APPROVED.equals(status);
    }

    private OfflinePayUtil() {
    }
}
