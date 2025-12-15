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
 * 巨量广告主账户表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_qc_oe_advertiser")
@ApiModel(value = "QcOeAdvertiser对象", description = "巨量广告主账户表")
public class QcOeAdvertiser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("广告主账户ID")
    private Long advertiserId;

    @ApiModelProperty("账户渠道：ADS=巨量广告, QIANCHUAN=千川")
    private String channel;

    @ApiModelProperty("广告主名称")
    private String advertiserName;

    @ApiModelProperty("账户状态, 如 STATUS_ENABLE/LIMIT/DISABLE")
    private String advertiserStatus;

    @ApiModelProperty("广告主主体ID")
    private Long advCompanyId;

    @ApiModelProperty("广告主体名称")
    private String advCompanyName;

    @ApiModelProperty("客户归属/老板ID")
    private Long ownerId;

    @ApiModelProperty("最近一次有消耗的日期")
    private Date lastCostDate;

    @ApiModelProperty("一级代理商ID")
    private Long firstAgentId;

    @ApiModelProperty("一级代理商名称")
    private String firstAgentName;

    @ApiModelProperty("一级代理商主体ID")
    private Long firstAgentCompanyId;

    @ApiModelProperty("一级代理商主体名称")
    private String firstAgentCompanyName;

    @ApiModelProperty("一级行业")
    private String firstIndustryName;

    @ApiModelProperty("二级行业")
    private String secondIndustryName;

    @ApiModelProperty("授权过期时间")
    private Date authExpireDate;

    @ApiModelProperty("关联客户公司表ID，指向wk_qc_oe_company.id")
    private Long companyId;

    @ApiModelProperty("代理商绑定时间")
    private Date bindTime;

    @ApiModelProperty("巨量侧创建时间")
    private Date createTimeOe;

    @ApiModelProperty("当前挂接销售ID(巨量sale_id)")
    private Long saleId;

    @ApiModelProperty("当前挂接销售姓名")
    private String saleName;

    @ApiModelProperty("对接联系人姓名")
    private String contactName;

    @ApiModelProperty("客户经理姓名（巨量字段 customer_sale_name）")
    private String customerSaleName;

    @ApiModelProperty("优化师ID")
    private Long optimizerId;

    @ApiModelProperty("优化师姓名")
    private String optimizerName;

    @ApiModelProperty("品牌优化师ID")
    private Long brandOptimizerId;

    @ApiModelProperty("品牌优化师姓名")
    private String brandOptimizerName;

    @ApiModelProperty("关联wk_qc_oe_sale_user.id")
    private Long saleUserId;

    @ApiModelProperty("自运营标签")
    private String selfOperationTag;

    @ApiModelProperty("账户来源(LOCAL等,可从select接口拿)")
    private String accountSource;

    @ApiModelProperty("最近一次同步时间")
    private Date lastSyncTime;

    @ApiModelProperty("创建时间")
    private Date gmtCreate;

    @ApiModelProperty("更新时间")
    private Date gmtModified;

    @ApiModelProperty("逻辑删除")
    private Integer isDeleted;
}
