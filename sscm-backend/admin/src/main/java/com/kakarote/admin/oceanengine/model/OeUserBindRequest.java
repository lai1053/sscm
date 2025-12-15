package com.kakarote.admin.oceanengine.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 绑定请求体。
 */
@Data
@ApiModel("用户绑定请求")
public class OeUserBindRequest {

    @ApiModelProperty(value = "WuKong 用户ID", required = true, example = "10001")
    private Long adminUserId;

    @ApiModelProperty(value = "巨量销售用户ID", required = true, example = "20002")
    private Long oeSaleUserId;
}
