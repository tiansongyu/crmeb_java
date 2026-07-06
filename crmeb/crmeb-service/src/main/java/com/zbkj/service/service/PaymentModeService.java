package com.zbkj.service.service;

import com.zbkj.common.request.PaymentModeSwitchRequest;
import com.zbkj.common.response.PaymentModeResponse;

/**
 * 支付模式服务
 */
public interface PaymentModeService {

    /**
     * 获取当前支付模式
     */
    PaymentModeResponse getMode();

    /**
     * 切换支付模式
     */
    PaymentModeResponse switchMode(PaymentModeSwitchRequest request);
}
