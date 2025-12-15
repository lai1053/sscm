package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售层级汇总
 */
@Data
public class SalesDashboardSaleVO {
    private Long saleUserId;
    private Long saleId;
    private String saleName;
    private Long crmUserId;
    private String crmRealname;
    private Integer advertiserCount;
    private Integer companyCount;
    private BigDecimal adsCost;
    private BigDecimal qcCost;
    private BigDecimal totalCost;
}
