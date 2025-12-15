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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 巨量广告主日报表（ADS）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_qc_oe_ads_advertiser_daily")
@ApiModel(value = "QcOeAdsAdvertiserDaily对象", description = "巨量广告主日报表（ADS）")
public class QcOeAdsAdvertiserDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("广告主ID")
    private Long advertiserId;

    @ApiModelProperty("统计日期")
    private Date statDate;

    @ApiModelProperty("广告主体ID")
    private Long advCompanyId;

    @ApiModelProperty("渠道，默认ADS")
    private String channel;

    @ApiModelProperty("总消耗")
    private BigDecimal statCost;

    @ApiModelProperty("展示数")
    private Long showCnt;

    @ApiModelProperty("点击数")
    private Long clickCnt;

    @ApiModelProperty("创建时间")
    private Date gmtCreate;

    @ApiModelProperty("更新时间")
    private Date gmtModified;
}
