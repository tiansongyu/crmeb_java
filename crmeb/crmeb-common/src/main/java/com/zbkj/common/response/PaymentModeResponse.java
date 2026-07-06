package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付模式响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "PaymentModeResponse对象", description = "支付模式响应")
public class PaymentModeResponse implements Serializable {

    private static final long serialVersionUID = 2644842530976580134L;

    @ApiModelProperty(value = "当前支付模式")
    private String mode;

    @ApiModelProperty(value = "当前支付模式名称")
    private String modeName;

    @ApiModelProperty(value = "扫码转账是否开启")
    private Boolean offlinePayOpen;

    @ApiModelProperty(value = "微信在线支付是否开启")
    private Boolean wechatPayOpen;

    @ApiModelProperty(value = "扫码转账收款码是否已配置")
    private Boolean offlinePayReady;

    @ApiModelProperty(value = "微信在线支付配置是否已配置")
    private Boolean wechatPayReady;

    @ApiModelProperty(value = "切换提示")
    private List<String> warnings = new ArrayList<>();
}
