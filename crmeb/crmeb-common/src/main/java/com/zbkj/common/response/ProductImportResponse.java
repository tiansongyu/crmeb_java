package com.zbkj.common.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "ProductImportResponse对象", description = "商品JSON导入结果")
public class ProductImportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "总行数")
    private Integer total = 0;

    @ApiModelProperty(value = "成功行数")
    private Integer success = 0;

    @ApiModelProperty(value = "失败行数")
    private Integer failed = 0;

    @ApiModelProperty(value = "是否为校验模式")
    private Boolean dryRun = true;

    @ApiModelProperty(value = "逐行结果")
    private List<ProductImportItemResponse> items = new ArrayList<>();
}
