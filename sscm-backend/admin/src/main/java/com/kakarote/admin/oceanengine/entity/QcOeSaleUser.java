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
 * 巨量销售用户表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_qc_oe_sale_user")
@ApiModel(value = "QcOeSaleUser对象", description = "巨量销售用户表")
public class QcOeSaleUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("巨量销售ID")
    private Long saleId;

    @ApiModelProperty("巨量销售姓名(中文)")
    private String saleName;

    @ApiModelProperty("CRM登录名(中文全拼, 重名+数字)")
    private String loginUsername;

    @ApiModelProperty("关联CRM用户表ID(如wk_admin_user.user_id)")
    private Long loginUserId;

    @ApiModelProperty("1启用 0禁用")
    private Integer status;

    @ApiModelProperty("来源")
    private String source;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private Date gmtCreate;

    @ApiModelProperty("更新时间")
    private Date gmtModified;

    @ApiModelProperty("逻辑删除")
    private Integer isDeleted;
}
