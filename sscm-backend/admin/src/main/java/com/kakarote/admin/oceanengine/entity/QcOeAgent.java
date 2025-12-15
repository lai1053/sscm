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
 * 巨量代理商账户表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_qc_oe_agent")
@ApiModel(value = "QcOeAgent对象", description = "巨量代理商账户表")
public class QcOeAgent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("巨量代理商广告账户ID (agent_id)")
    private Long agentId;

    @ApiModelProperty("代理商名称")
    private String agentName;

    @ApiModelProperty("角色, 如 ROLE_AGENT")
    private String role;

    @ApiModelProperty("账户状态,如 STATUS_ENABLE")
    private String accountStatus;

    @ApiModelProperty("主体ID company_id")
    private Long companyId;

    @ApiModelProperty("主体名称")
    private String companyName;

    @ApiModelProperty("客户中心ID customer_id")
    private Long customerId;

    @ApiModelProperty("客户中心名称")
    private String customerName;

    @ApiModelProperty("巨量侧create_time")
    private Date oeCreateTime;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private Date gmtCreate;

    @ApiModelProperty("更新时间")
    private Date gmtModified;

    @ApiModelProperty("逻辑删除 0正常 1删除")
    private Integer isDeleted;
}
