package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * 支付模式切换请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "PaymentModeSwitchRequest对象", description = "支付模式切换")
public class PaymentModeSwitchRequest {

    @ApiModelProperty(value = "支付模式：offline_qr-扫码转账，wechat_online-微信在线支付")
    @NotBlank(message = "支付模式不能为空")
    private String mode;
}
