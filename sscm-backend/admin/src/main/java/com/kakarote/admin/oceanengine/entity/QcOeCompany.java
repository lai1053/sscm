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
 * 巨量客户公司表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wk_qc_oe_company")
@ApiModel(value = "QcOeCompany对象", description = "巨量客户公司表")
public class QcOeCompany implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("巨量客户公司ID，对应 adv_company_id")
    private Long advCompanyId;

    @ApiModelProperty("客户公司名称，对应 adv_company_name")
    private String advCompanyName;

    @ApiModelProperty("主行业名称")
    private String firstIndustryName;

    @ApiModelProperty("子行业名称")
    private String secondIndustryName;

    @ApiModelProperty("创建时间")
    private Date gmtCreate;

    @ApiModelProperty("更新时间")
    private Date gmtModified;

    @ApiModelProperty("逻辑删除")
    private Integer isDeleted;
}
