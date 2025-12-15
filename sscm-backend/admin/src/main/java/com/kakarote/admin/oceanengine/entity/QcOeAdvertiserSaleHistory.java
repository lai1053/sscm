package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 广告主销售变更历史
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_qc_oe_advertiser_sale_history")
@ApiModel(value = "QcOeAdvertiserSaleHistory对象", description = "广告主销售变更历史")
public class QcOeAdvertiserSaleHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("广告主账户ID")
    private Long advertiserId;

    @ApiModelProperty("变更前销售ID")
    @TableField("prev_sale_id")
    private Long prevSaleId;

    @ApiModelProperty("变更前销售姓名")
    @TableField("prev_sale_name")
    private String prevSaleName;

    @ApiModelProperty("变更后销售ID")
    private Long newSaleId;

    @ApiModelProperty("变更后销售姓名")
    private String newSaleName;

    @ApiModelProperty("变更时间(以巨量返回时间为准或本地时间)")
    private Date changeTime;

    @ApiModelProperty("来源: SYNC/CRM_MANUAL 等")
    private String changeSource;

    @ApiModelProperty("操作人(可为空)")
    private String operator;

    @ApiModelProperty("创建时间")
    private Date gmtCreate;

    @ApiModelProperty("更新时间")
    private Date gmtModified;

    @ApiModelProperty("逻辑删除")
    private Integer isDeleted;
}
