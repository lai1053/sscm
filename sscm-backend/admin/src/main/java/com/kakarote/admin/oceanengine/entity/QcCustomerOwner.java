package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户归属/老板主体表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_qc_customer_owner")
@ApiModel(value = "QcCustomerOwner对象", description = "客户归属/老板主体表")
public class QcCustomerOwner implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("老板姓名")
    private String ownerName;

    @ApiModelProperty("手机号")
    private String mobile;

    @ApiModelProperty("微信号")
    private String wechat;

    @ApiModelProperty("等级")
    private String level;

    @ApiModelProperty("标签")
    private String tags;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("逻辑删除标识")
    private Integer isDeleted;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;
}
