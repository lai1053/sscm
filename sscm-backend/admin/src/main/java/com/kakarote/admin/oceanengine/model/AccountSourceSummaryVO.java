package com.kakarote.admin.oceanengine.model;

import lombok.Data;

/**
 * 账户来源维度汇总
 */
@Data
public class AccountSourceSummaryVO {
    private String accountSource;
    private Integer companyCount;
    private Integer advertiserCount;
    private Integer unassignedAdvertiserCount;
    private Integer unassignedCompanyCount;
}
