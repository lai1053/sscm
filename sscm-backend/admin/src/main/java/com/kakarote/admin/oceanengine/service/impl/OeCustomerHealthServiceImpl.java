package com.kakarote.admin.oceanengine.service.impl;

import com.kakarote.admin.oceanengine.enums.CustomerHealthStatus;
import com.kakarote.admin.oceanengine.service.OeDailyQueryService;
import com.kakarote.admin.oceanengine.service.OeCustomerHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户健康度计算（简单版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeCustomerHealthServiceImpl implements OeCustomerHealthService {

    private final OeDailyQueryService dailyQueryService;

    @Override
    public CustomerHealthStatus calculateHealth(Long advCompanyId, LocalDate baseDate) {
        if (advCompanyId == null || baseDate == null) {
            return CustomerHealthStatus.INACTIVE_90D;
        }
        // 近7天有消耗 → ACTIVE
        if (hasCostWithinDays(advCompanyId, baseDate, 7)) {
            return CustomerHealthStatus.ACTIVE;
        }
        // 近30天有消耗 → 标记为 INACTIVE_30D
        if (hasCostWithinDays(advCompanyId, baseDate, 30)) {
            return CustomerHealthStatus.INACTIVE_30D;
        }
        // 否则 INACTIVE_90D
        return CustomerHealthStatus.INACTIVE_90D;
    }

    private boolean hasCostWithinDays(Long advCompanyId, LocalDate baseDate, int days) {
        if (advCompanyId == null || baseDate == null || days <= 0) {
            return false;
        }
        LocalDate startDate = baseDate.minusDays(days - 1L);
        BigDecimal adsCost = dailyQueryService.sumAdsCostByCompany(advCompanyId, startDate, baseDate);
        BigDecimal qcCost = dailyQueryService.sumQcCostByCompany(advCompanyId, startDate, baseDate);
        BigDecimal total = (adsCost == null ? BigDecimal.ZERO : adsCost)
                .add(qcCost == null ? BigDecimal.ZERO : qcCost);
        return total.compareTo(BigDecimal.ZERO) > 0;
    }
}
