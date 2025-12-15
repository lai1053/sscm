package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BossCostTrendVO {
    private LocalDate statDate;
    private BigDecimal adsCost;
    private BigDecimal qcCost;
    private BigDecimal totalCost;
}
