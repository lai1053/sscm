package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.sql.Date;

@Data
public class CompanyLastCostRow {
    private Long advCompanyId;
    private Date lastCostDate;
}
