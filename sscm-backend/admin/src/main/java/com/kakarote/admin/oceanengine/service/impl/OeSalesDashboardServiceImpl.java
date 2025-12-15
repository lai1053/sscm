package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeSaleUserMapper;
import com.kakarote.admin.oceanengine.model.SalesAdvertiserVO;
import com.kakarote.admin.oceanengine.model.AccountSourceSummaryVO;
import com.kakarote.admin.oceanengine.model.SalesCompanySummaryVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardAdvertiserVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardCompanyVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardSaleVO;
import com.kakarote.admin.oceanengine.model.SalesOverviewCompanyRow;
import com.kakarote.admin.oceanengine.model.SalesOverviewVO;
import com.kakarote.admin.oceanengine.model.CompanyLastCostRow;
import com.kakarote.admin.oceanengine.model.CustomerTrendPointVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardSummaryVO;
import com.kakarote.admin.oceanengine.service.OeDashboardScopeService;
import com.kakarote.admin.oceanengine.service.OeSalesDashboardService;
import com.kakarote.core.entity.BasePage;
import com.kakarote.core.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销售看板基础服务
 */
@Service
@RequiredArgsConstructor
public class OeSalesDashboardServiceImpl implements OeSalesDashboardService {

    private final QcOeSaleUserMapper saleUserMapper;
    private final QcOeAdvertiserMapper advertiserMapper;
    private final com.kakarote.admin.oceanengine.mapper.QcOeAdsAdvertiserDailyMapper adsDailyMapper;
    private final com.kakarote.admin.oceanengine.mapper.QcOeQcAdvertiserDailyMapper qcDailyMapper;
    private final OeDashboardScopeService dashboardScopeService;

    @Override
    public BasePage<SalesDashboardSummaryVO> listSalesSummary(Integer page, Integer limit, @Nullable String keyword) {
        BasePage<SalesDashboardSummaryVO> basePage = new BasePage<>(page == null ? 1 : page, limit == null ? 15 : limit);
        basePage.setList(saleUserMapper.listSalesSummary(basePage, keyword));
        return basePage;
    }

    @Override
    public BasePage<SalesAdvertiserVO> listAdvertisersBySaleUser(Long saleUserId, Integer page, Integer limit, @Nullable String channel) {
        List<Long> effectiveSaleUserIds = resolveSaleUserIds(saleUserId);
        BasePage<SalesAdvertiserVO> basePage = new BasePage<>(page == null ? 1 : page, limit == null ? 15 : limit);
        if (effectiveSaleUserIds != null && effectiveSaleUserIds.isEmpty()) {
            basePage.setList(new ArrayList<>());
            return basePage;
        }
        basePage.setList(advertiserMapper.listAdvertisersBySaleUser(basePage, effectiveSaleUserIds, channel));
        return basePage;
    }

    @Override
    public List<SalesCompanySummaryVO> summaryBySaleUser(Long saleUserId, LocalDate startDate, LocalDate endDate) {
        List<Long> effectiveSaleUserIds = resolveSaleUserIds(saleUserId);
        if (effectiveSaleUserIds != null && effectiveSaleUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (startDate == null || endDate == null) {
            return new ArrayList<>();
        }
        Date start = Date.valueOf(startDate);
        Date end = Date.valueOf(endDate);
        List<Map<String, Object>> adsRows = adsDailyMapper.selectCostByCompanyAndSaleUser(start, end, effectiveSaleUserIds);
        List<Map<String, Object>> qcRows = qcDailyMapper.selectCostByCompanyAndSaleUser(start, end, effectiveSaleUserIds);

        Map<Long, SalesCompanySummaryVO> map = new HashMap<>();
        mergeCost(map, adsRows, true);
        mergeCost(map, qcRows, false);

        List<SalesCompanySummaryVO> result = new ArrayList<>(map.values());
        result.forEach(vo -> vo.setTotalCost(
                (vo.getAdsCost() == null ? BigDecimal.ZERO : vo.getAdsCost())
                        .add(vo.getQcCost() == null ? BigDecimal.ZERO : vo.getQcCost())
        ));
        return result;
    }

    private void mergeCost(Map<Long, SalesCompanySummaryVO> target, List<Map<String, Object>> rows, boolean isAds) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Object companyIdObj = row.get("advCompanyId");
            if (companyIdObj == null) {
                continue;
            }
            Long companyId = toLong(companyIdObj);
            if (companyId == null) {
                continue;
            }
            SalesCompanySummaryVO vo = target.computeIfAbsent(companyId, k -> new SalesCompanySummaryVO());
            vo.setAdvCompanyId(companyId);
            if (vo.getAdvCompanyName() == null) {
                vo.setAdvCompanyName(toString(row.get("advCompanyName")));
            }
            BigDecimal cost = toBigDecimal(row.get("totalCost"));
            if (isAds) {
                vo.setAdsCost(cost);
            } else {
                vo.setQcCost(cost);
            }
        }
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (obj instanceof Number) {
            return BigDecimal.valueOf(((Number) obj).doubleValue());
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String toString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private Map<Long, BigDecimal> toCompanyCostMap(List<Map<String, Object>> rows, String companyKey, String costKey) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Long companyId = toLong(row.get(companyKey));
            BigDecimal cost = toBigDecimal(row.get(costKey));
            if (companyId == null || cost == null) {
                continue;
            }
            map.put(companyId, cost);
        }
        return map;
    }

    @Override
    public List<SalesCompanySummaryVO> inactiveCompaniesBySaleUser(Long saleUserId, int days) {
        List<Long> effectiveSaleUserIds = resolveSaleUserIds(saleUserId);
        if (effectiveSaleUserIds != null && effectiveSaleUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        int period = days > 0 ? days : 30;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period - 1);

        QueryWrapper<QcOeAdvertiser> baseWrapper = new QueryWrapper<QcOeAdvertiser>()
                .select("adv_company_id as advCompanyId", "adv_company_name as advCompanyName")
                .eq("is_deleted", 0)
                .isNotNull("adv_company_id")
                .gt("adv_company_id", 0)
                .groupBy("adv_company_id", "adv_company_name");
        if (effectiveSaleUserIds != null) {
            baseWrapper.in("sale_user_id", effectiveSaleUserIds);
        }
        List<Map<String, Object>> advRows = advertiserMapper.selectMaps(baseWrapper);
        if (advRows == null || advRows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> companyIds = advRows.stream()
                .map(r -> toLong(r.get("advCompanyId")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (companyIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, BigDecimal> adsCost = toCompanyCostMap(adsDailyMapper.selectCostByCompanyFiltered(Date.valueOf(startDate), Date.valueOf(endDate), companyIds), "advCompanyId", "totalCost");
        Map<Long, BigDecimal> qcCost = toCompanyCostMap(qcDailyMapper.selectCostByCompanyFiltered(Date.valueOf(startDate), Date.valueOf(endDate), companyIds), "advCompanyId", "totalCost");

        List<SalesCompanySummaryVO> result = new ArrayList<>();
        for (Map<String, Object> row : advRows) {
            Long companyId = toLong(row.get("advCompanyId"));
            if (companyId == null) {
                continue;
            }
            BigDecimal ads = adsCost.getOrDefault(companyId, BigDecimal.ZERO);
            BigDecimal qc = qcCost.getOrDefault(companyId, BigDecimal.ZERO);
            if (ads.add(qc).compareTo(BigDecimal.ZERO) > 0) {
                continue;
            }
            SalesCompanySummaryVO vo = new SalesCompanySummaryVO();
            vo.setAdvCompanyId(companyId);
            vo.setAdvCompanyName(toString(row.get("advCompanyName")));
            vo.setAdsCost(ads);
            vo.setQcCost(qc);
            vo.setTotalCost(BigDecimal.ZERO);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<AccountSourceSummaryVO> listAccountSourceSummary() {
        return advertiserMapper.listAccountSourceSummary();
    }

    @Override
    public List<SalesDashboardSaleVO> dashboardSales(int window, String channel, String accountSource) {
        DateRange range = buildRange(window);
        return advertiserMapper.listSalesDashboard(range.getStart(), range.getEnd(), normalizeChannel(channel), normalizeAccountSource(accountSource));
    }

    @Override
    public List<SalesDashboardCompanyVO> dashboardCompanies(Long saleUserId, int window, String channel, String accountSource) {
        List<Long> effectiveSaleUserIds = resolveSaleUserIds(saleUserId);
        if (effectiveSaleUserIds != null && effectiveSaleUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        DateRange range = buildRange(window);
        List<SalesDashboardCompanyVO> list = advertiserMapper.listCompanyDashboard(effectiveSaleUserIds, range.getStart(), range.getEnd(), normalizeChannel(channel), normalizeAccountSource(accountSource));
        for (SalesDashboardCompanyVO vo : list) {
            fillRiskInfo(vo.getLastCostDate(), vo);
        }
        return list;
    }

    @Override
    public List<SalesDashboardAdvertiserVO> dashboardAdvertisers(Long saleUserId, Long advCompanyId, int window, String channel, String accountSource) {
        List<Long> effectiveSaleUserIds = resolveSaleUserIds(saleUserId);
        if (effectiveSaleUserIds != null && effectiveSaleUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (effectiveSaleUserIds == null || advCompanyId == null) {
            return new ArrayList<>();
        }
        DateRange range = buildRange(window);
        List<SalesDashboardAdvertiserVO> list = advertiserMapper.listAdvertiserDashboard(effectiveSaleUserIds, advCompanyId, range.getStart(), range.getEnd(), normalizeChannel(channel), normalizeAccountSource(accountSource));
        for (SalesDashboardAdvertiserVO vo : list) {
            fillRiskInfo(vo.getLastCostDate(), vo);
        }
        return list;
    }

    @Override
    public SalesOverviewVO overview(Integer window, String channel, String accountSource, Long saleUserId) {
        List<Long> effectiveSaleUserIds = resolveSaleUserIds(saleUserId);
        if (effectiveSaleUserIds != null && effectiveSaleUserIds.isEmpty()) {
            return new SalesOverviewVO();
        }
        int win = (window == null || window <= 0) ? 7 : window;
        DateRange current = buildRange(win);
        LocalDate prevStartDate = current.getStart().toLocalDate().minusDays(win);
        LocalDate prevEndDate = current.getStart().toLocalDate().minusDays(1);

        String ch = normalizeChannel(channel);
        String src = normalizeAccountSource(accountSource);

        List<SalesOverviewCompanyRow> currentRows = advertiserMapper.listCompanyCostForOverview(current.getStart(), current.getEnd(), ch, src, effectiveSaleUserIds);
        List<SalesOverviewCompanyRow> prevRows = advertiserMapper.listCompanyCostForOverview(Date.valueOf(prevStartDate), Date.valueOf(prevEndDate), ch, src, effectiveSaleUserIds);

        SalesOverviewVO vo = new SalesOverviewVO();
        vo.setWindowDays(win);
        vo.setWindowStart(current.getStart());
        vo.setWindowEnd(current.getEnd());

        vo.setAdsCost(sumCost(currentRows, true));
        vo.setQcCost(sumCost(currentRows, false));
        vo.setTotalCost(nullSafe(vo.getAdsCost()).add(nullSafe(vo.getQcCost())));

        vo.setPrevAdsCost(sumCost(prevRows, true));
        vo.setPrevQcCost(sumCost(prevRows, false));
        vo.setPrevTotalCost(nullSafe(vo.getPrevAdsCost()).add(nullSafe(vo.getPrevQcCost())));

        vo.setTotalCompanyCount(currentRows == null ? 0 : currentRows.size());
        vo.setTotalAdvertiserCount(sumAdvertiser(currentRows));

        Map<Long, SalesOverviewCompanyRow> currentMap = toCompanyRowMap(currentRows);
        Map<Long, BigDecimal> prevCostMap = buildCompanyCostMap(prevRows);
        vo.setDownCompanyCount(calcDownCompanyCount(currentMap, prevCostMap));

        List<CompanyLastCostRow> riskRows = advertiserMapper.listCompanyLastCostDate(ch, src, effectiveSaleUserIds);
        int active = 0, warn = 0, danger = 0;
        for (CompanyLastCostRow row : riskRows) {
            String level = buildRiskLevel(row.getLastCostDate());
            if ("ACTIVE".equals(level)) {
                active++;
            } else if ("WARN".equals(level)) {
                warn++;
            } else {
                danger++;
            }
        }
        vo.setActiveCompanyCount(active);
        vo.setWarnCompanyCount(warn);
        vo.setDangerCompanyCount(danger);

        return vo;
    }

    @Override
    public List<CustomerTrendPointVO> customerTrend(Long advCompanyId, Integer window, String channel, String accountSource) {
        if (advCompanyId == null || advCompanyId <= 0) {
            return new ArrayList<>();
        }
        int win = (window == null || window <= 0) ? 30 : window;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(win - 1L);
        List<CustomerTrendPointVO> list = advertiserMapper.listCustomerTrend(advCompanyId,
                Date.valueOf(start),
                Date.valueOf(end),
                normalizeChannel(channel),
                normalizeAccountSource(accountSource));
        if (list == null) {
            return new ArrayList<>();
        }
        for (CustomerTrendPointVO p : list) {
            BigDecimal ads = nullSafe(p.getAdsCost());
            BigDecimal qc = nullSafe(p.getQcCost());
            p.setTotalCost(ads.add(qc));
        }
        return list;
    }

    private DateRange buildRange(int window) {
        int days = window > 0 ? window : 7;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        return new DateRange(Date.valueOf(startDate), Date.valueOf(endDate));
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            return "ALL";
        }
        String upper = channel.trim().toUpperCase();
        if ("ADS".equals(upper) || "QIANCHUAN".equals(upper)) {
            return upper;
        }
        return "ALL";
    }

    private String normalizeAccountSource(String accountSource) {
        if (accountSource == null) {
            return "ALL";
        }
        String upper = accountSource.trim().toUpperCase();
        if ("AD".equals(upper) || "LOCAL".equals(upper)) {
            return upper;
        }
        return "ALL";
    }

    private BigDecimal sumCost(List<SalesOverviewCompanyRow> rows, boolean ads) {
        BigDecimal total = BigDecimal.ZERO;
        if (rows == null) {
            return total;
        }
        for (SalesOverviewCompanyRow row : rows) {
            BigDecimal cost = ads ? row.getAdsCost() : row.getQcCost();
            total = total.add(nullSafe(cost));
        }
        return total;
    }

    private int sumAdvertiser(List<SalesOverviewCompanyRow> rows) {
        if (rows == null) {
            return 0;
        }
        int total = 0;
        for (SalesOverviewCompanyRow row : rows) {
            if (row.getAdvertiserCount() != null) {
                total += row.getAdvertiserCount();
            }
        }
        return total;
    }

    private Map<Long, SalesOverviewCompanyRow> toCompanyRowMap(List<SalesOverviewCompanyRow> rows) {
        Map<Long, SalesOverviewCompanyRow> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (SalesOverviewCompanyRow row : rows) {
            if (row.getAdvCompanyId() == null) {
                continue;
            }
            map.put(row.getAdvCompanyId(), row);
        }
        return map;
    }

    private Map<Long, BigDecimal> buildCompanyCostMap(List<SalesOverviewCompanyRow> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (SalesOverviewCompanyRow row : rows) {
            if (row.getAdvCompanyId() == null) {
                continue;
            }
            map.put(row.getAdvCompanyId(), nullSafe(row.getTotalCost()));
        }
        return map;
    }

    private int calcDownCompanyCount(Map<Long, SalesOverviewCompanyRow> current, Map<Long, BigDecimal> prev) {
        int count = 0;
        for (Map.Entry<Long, SalesOverviewCompanyRow> entry : current.entrySet()) {
            Long companyId = entry.getKey();
            BigDecimal cur = nullSafe(entry.getValue().getTotalCost());
            BigDecimal pre = prev.get(companyId);
            if (pre == null || pre.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal rate = cur.subtract(pre).divide(pre, 4, BigDecimal.ROUND_HALF_UP);
            if (rate.compareTo(new BigDecimal("-0.1")) <= 0) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal nullSafe(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    private void fillRiskInfo(java.util.Date lastCostDate, SalesDashboardCompanyVO vo) {
        RiskInfo info = buildRiskInfo(lastCostDate);
        vo.setLastCostDate(info.lastCostDate);
        vo.setInactiveDays(info.inactiveDays);
        vo.setRiskLevel(info.riskLevel);
    }

    private void fillRiskInfo(java.util.Date lastCostDate, SalesDashboardAdvertiserVO vo) {
        RiskInfo info = buildRiskInfo(lastCostDate);
        vo.setLastCostDate(info.lastCostDate);
        vo.setInactiveDays(info.inactiveDays);
        vo.setRiskLevel(info.riskLevel);
    }

    private RiskInfo buildRiskInfo(java.util.Date lastCostDate) {
        RiskInfo info = new RiskInfo();
        if (lastCostDate == null) {
            info.lastCostDate = null;
            info.inactiveDays = null;
            info.riskLevel = "DANGER";
            return info;
        }
        info.lastCostDate = lastCostDate;
        LocalDate today = LocalDate.now();
        LocalDate lastDate;
        // 兼容 java.sql.Date，避免直接调用 java.sql.Date#toInstant 抛 UnsupportedOperationException
        if (lastCostDate instanceof java.sql.Date) {
            lastDate = ((java.sql.Date) lastCostDate).toLocalDate();
        } else {
            lastDate = lastCostDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        int days = (int) ChronoUnit.DAYS.between(lastDate, today);
        info.inactiveDays = days;
        if (days <= 15) {
            info.riskLevel = "ACTIVE";
        } else if (days <= 30) {
            info.riskLevel = "WARN";
        } else {
            info.riskLevel = "DANGER";
        }
        return info;
    }

    private String buildRiskLevel(java.sql.Date lastCostDate) {
        // 复用 buildRiskInfo，内部已处理 java.sql.Date
        RiskInfo info = buildRiskInfo(lastCostDate);
        return info.riskLevel;
    }

    private String buildRiskLevel(java.util.Date lastCostDate) {
        RiskInfo info = buildRiskInfo(lastCostDate);
        return info.riskLevel;
    }

    private List<Long> resolveSaleUserIds(Long saleUserId) {
        if (UserUtil.isAdmin()) {
            if (saleUserId == null) {
                return null;
            }
            return Collections.singletonList(saleUserId);
        }
        List<Long> scoped = dashboardScopeService.resolveEffectiveSaleUserIds(saleUserId);
        if (scoped == null || scoped.isEmpty()) {
            return scoped;
        }
        return scoped;
    }

    private static class RiskInfo {
        private java.util.Date lastCostDate;
        private Integer inactiveDays;
        private String riskLevel;
    }

    private static class DateRange {
        private final Date start;
        private final Date end;

        DateRange(Date start, Date end) {
            this.start = start;
            this.end = end;
        }

        Date getStart() {
            return start;
        }

        Date getEnd() {
            return end;
        }
    }
}
