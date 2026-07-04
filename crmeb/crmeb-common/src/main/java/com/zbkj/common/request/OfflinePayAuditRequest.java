package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 线下扫码转账审核
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "OfflinePayAuditRequest对象", description = "线下扫码转账审核")
public class OfflinePayAuditRequest {

    @ApiModelProperty(value = "订单编号")
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @ApiModelProperty(value = "是否审核通过")
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @ApiModelProperty(value = "驳回原因")
    private String reason;
}
