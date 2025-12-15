package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOverviewCompanyRow {
    private Long advCompanyId;
    private BigDecimal adsCost;
    private BigDecimal qcCost;
    private BigDecimal totalCost;
    private Integer advertiserCount;
}
