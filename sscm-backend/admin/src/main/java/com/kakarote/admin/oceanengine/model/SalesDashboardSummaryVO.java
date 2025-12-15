package com.kakarote.admin.oceanengine.model;

import lombok.Data;

/**
 * 销售汇总视图
 */
@Data
public class SalesDashboardSummaryVO {
    private Long saleUserId;
    private Long saleId;
    private String saleName;
    private Long crmUserId;
    private String crmUsername;
    private String crmRealname;
    private Integer totalAdvertiserCount;
    private Integer adsAdvertiserCount;
    private Integer qianchuanAdvertiserCount;
    private Integer totalCompanyCount;
    private Integer adsCompanyCount;
    private Integer qianchuanCompanyCount;
    private Integer advertiserCountAd;
    private Integer advertiserCountStar;
    private Integer advertiserCountLuban;
    private Integer advertiserCountDomestic;
    private Integer advertiserCountLocal;
    private Integer companyCountAd;
    private Integer companyCountStar;
    private Integer companyCountLuban;
    private Integer companyCountDomestic;
    private Integer companyCountLocal;
}
