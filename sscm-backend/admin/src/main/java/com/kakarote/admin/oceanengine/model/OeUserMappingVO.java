package com.kakarote.admin.oceanengine.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户与巨量销售绑定关系视图。
 */
@Data
@ApiModel("用户与巨量销售绑定关系")
public class OeUserMappingVO {

    @ApiModelProperty("绑定记录ID")
    private Long id;

    @ApiModelProperty("WuKong 用户ID")
    private Long adminUserId;

    @ApiModelProperty("巨量销售用户ID")
    private Long oeSaleUserId;

    @ApiModelProperty("操作人ID")
    private Long operatorId;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 预留展示用字段，可后续补充。
     */
    @ApiModelProperty("WuKong 用户姓名（预留）")
    private String adminUserName;

    @ApiModelProperty("巨量销售姓名（预留）")
    private String oeSaleUserName;
}
