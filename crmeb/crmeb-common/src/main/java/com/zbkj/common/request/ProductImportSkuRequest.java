package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ProductImportSkuRequest对象", description = "商品JSON导入SKU")
public class ProductImportSkuRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "规格键值")
    private LinkedHashMap<String, String> specs;

    @ApiModelProperty(value = "售价")
    private BigDecimal price;

    @ApiModelProperty(value = "原价")
    private BigDecimal otPrice;

    @ApiModelProperty(value = "成本价")
    private BigDecimal cost;

    @ApiModelProperty(value = "库存")
    private Integer stock;

    @ApiModelProperty(value = "重量")
    private BigDecimal weight;

    @ApiModelProperty(value = "体积")
    private BigDecimal volume;

    @ApiModelProperty(value = "SKU图片")
    private String image;

    @ApiModelProperty(value = "一级返佣")
    private BigDecimal brokerage;

    @ApiModelProperty(value = "二级返佣")
    private BigDecimal brokerageTwo;

    @ApiModelProperty(value = "商品条码")
    private String barCode;
}
