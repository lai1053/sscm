package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.model.BossCostTrendVO;
import com.kakarote.admin.oceanengine.model.InactiveCompanyVO;

import java.time.LocalDate;
import java.util.List;

public interface OeBossDashboardService {

    /**
     * 按天聚合总消耗趋势
     */
    List<BossCostTrendVO> listCostTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 近 N 天无消耗公司列表（默认30天）
     */
    List<InactiveCompanyVO> listInactiveCompanies(int days);
}
