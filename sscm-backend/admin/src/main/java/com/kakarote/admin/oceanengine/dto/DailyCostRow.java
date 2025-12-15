package com.kakarote.admin.oceanengine.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日报按天消耗明细 DTO（ADS/QC 通用）。
 */
@Data
public class DailyCostRow {
    private LocalDate statDate;
    private BigDecimal statCost;
    private Long showCnt;
    private Long clickCnt;
    private Long payOrderCount;
    private BigDecimal payGmv;
    private BigDecimal roi;
}
