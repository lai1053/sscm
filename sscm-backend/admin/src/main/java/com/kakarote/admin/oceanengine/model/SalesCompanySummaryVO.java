package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesCompanySummaryVO {
    private Long advCompanyId;
    private String advCompanyName;
    private BigDecimal adsCost;
    private BigDecimal qcCost;
    private BigDecimal totalCost;
}
