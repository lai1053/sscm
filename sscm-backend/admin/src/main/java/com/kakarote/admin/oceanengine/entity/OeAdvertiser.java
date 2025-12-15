
package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author dong
 * @date 2025-11-25 16:55
 */
@Data
@TableName("wk_qc_oe_advertiser")
public class OeAdvertiser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long advertiserId;
    private String advertiserName;
    private String advertiserStatus;

    private Long advCompanyId;
    private String advCompanyName;

    private Long firstAgentId;
    private String firstAgentName;
    private Long firstAgentCompanyId;
    private String firstAgentCompanyName;

    private String firstIndustryName;
    private String secondIndustryName;

    private String contactName;
    private String customerSaleName;
    private Long optimizerId;
    private String optimizerName;
    private Long brandOptimizerId;
    private String brandOptimizerName;

    private LocalDate authExpireDate;
    private LocalDateTime bindTime;
    private LocalDateTime createTimeOe;

    private Long saleId;
    private String saleName;
    private Long saleUserId;

    private String selfOperationTag;
    private String accountSource;
    private LocalDateTime lastSyncTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer isDeleted;
}
