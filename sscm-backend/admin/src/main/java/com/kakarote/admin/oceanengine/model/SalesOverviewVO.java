package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
public class SalesOverviewVO {
    private Integer windowDays;
    private Date windowStart;
    private Date windowEnd;

    private BigDecimal totalCost;
    private BigDecimal adsCost;
    private BigDecimal qcCost;

    private BigDecimal prevTotalCost;
    private BigDecimal prevAdsCost;
    private BigDecimal prevQcCost;

    private Integer totalCompanyCount;
    private Integer totalAdvertiserCount;

    private Integer activeCompanyCount;
    private Integer warnCompanyCount;
    private Integer dangerCompanyCount;

    private Integer downCompanyCount;
}
