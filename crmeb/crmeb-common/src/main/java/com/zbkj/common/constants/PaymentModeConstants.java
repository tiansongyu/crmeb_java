package com.zbkj.common.constants;

/**
 * 支付模式常量
 */
public class PaymentModeConstants {

    public static final String CONFIG_PAY_MODE = "pay_mode";

    public static final String MODE_OFFLINE_QR = "offline_qr";
    public static final String MODE_WECHAT_ONLINE = "wechat_online";

    public static final String MODE_NAME_OFFLINE_QR = "扫码转账";
    public static final String MODE_NAME_WECHAT_ONLINE = "微信在线支付";

    private PaymentModeConstants() {
    }
}
