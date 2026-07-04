package com.zbkj.common.constants;

/**
 * 线下扫码转账支付常量
 */
public class OfflinePayConstants {

    /** 未提交付款凭证 */
    public static final Integer STATUS_NOT_SUBMITTED = 0;

    /** 已提交付款凭证，待后台审核 */
    public static final Integer STATUS_PENDING = 1;

    /** 后台审核通过 */
    public static final Integer STATUS_APPROVED = 2;

    /** 后台审核驳回 */
    public static final Integer STATUS_REJECTED = 3;

    public static final String STATUS_TEXT_NOT_SUBMITTED = "未提交";
    public static final String STATUS_TEXT_PENDING = "待审核";
    public static final String STATUS_TEXT_APPROVED = "已通过";
    public static final String STATUS_TEXT_REJECTED = "已驳回";

    public static final String CONFIG_OFFLINE_PAY_STATUS = "offline_pay_status";
    public static final String CONFIG_OFFLINE_PAY_QRCODE = "offline_pay_qrcode";
    public static final String CONFIG_OFFLINE_PAY_NAME = "offline_pay_name";
    public static final String CONFIG_OFFLINE_PAY_TIPS = "offline_pay_tips";

    private OfflinePayConstants() {
    }
}
