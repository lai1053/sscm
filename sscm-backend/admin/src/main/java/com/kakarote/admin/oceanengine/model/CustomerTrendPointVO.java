package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
public class CustomerTrendPointVO {
    private Date statDate;
    private BigDecimal adsCost;
    private BigDecimal qcCost;
    private BigDecimal totalCost;
}
