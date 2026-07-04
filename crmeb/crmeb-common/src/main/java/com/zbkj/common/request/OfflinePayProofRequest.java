package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * 线下扫码转账付款凭证
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "OfflinePayProofRequest对象", description = "线下扫码转账付款凭证")
public class OfflinePayProofRequest {

    @ApiModelProperty(value = "订单编号")
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @ApiModelProperty(value = "付款凭证图片")
    @NotBlank(message = "付款凭证不能为空")
    private String voucher;

    @ApiModelProperty(value = "付款交易号")
    private String tradeNo;

    @ApiModelProperty(value = "付款备注")
    private String remark;
}
