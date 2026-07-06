package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ProductImportItemResponse对象", description = "商品JSON导入单项结果")
public class ProductImportItemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "行号")
    private Integer row;

    @ApiModelProperty(value = "商品名称")
    private String storeName;

    @ApiModelProperty(value = "是否成功")
    private Boolean success;

    @ApiModelProperty(value = "商品ID")
    private Integer productId;

    @ApiModelProperty(value = "结果信息")
    private String message;
}
