package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 销售-公司-广告主层级视图
 */
@Data
public class SalesDashboardAdvertiserVO {
    private Long saleUserId;
    private String saleName;
    private Long advCompanyId;
    private String advCompanyName;
    private Long advertiserId;
    private String advertiserName;
    private String channel;
    private String accountSource;
    /**
     * 最近一次有消耗的日期（来自广告主 last_cost_date）
     */
    private Date lastCostDate;
    /**
     * 不活跃天数（今天 - lastCostDate），空表示未知
     */
    private Integer inactiveDays;
    /**
     * 风险等级：
     * ACTIVE  - 最近 15 天内有消耗
     * WARN    - 15 ~ 30 天未消耗
     * DANGER  - 超过 30 天未消耗或从未有过消耗
     */
    private String riskLevel;
    private BigDecimal adsCost;
    private BigDecimal qcCost;
    private BigDecimal totalCost;
}
