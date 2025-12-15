package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kakarote.admin.oceanengine.entity.QcOeAdsAdvertiserDaily;
import com.kakarote.admin.oceanengine.entity.QcOeQcAdvertiserDaily;
import com.kakarote.admin.oceanengine.entity.QcOeCompany;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeAdsAdvertiserDailyMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeQcAdvertiserDailyMapper;
import com.kakarote.admin.oceanengine.model.BossCostTrendVO;
import com.kakarote.admin.oceanengine.model.InactiveCompanyVO;
import com.kakarote.admin.oceanengine.service.OeBossDashboardService;
import com.kakarote.admin.oceanengine.service.IQcOeCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OeBossDashboardServiceImpl implements OeBossDashboardService {

    private final QcOeAdsAdvertiserDailyMapper adsDailyMapper;
    private final QcOeQcAdvertiserDailyMapper qcDailyMapper;
    private final IQcOeCompanyService companyService;
    private final QcOeAdvertiserMapper advertiserMapper;

    @Override
    public List<BossCostTrendVO> listCostTrend(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return new ArrayList<>();
        }
        Map<LocalDate, BigDecimal> adsMap = aggregateAds(startDate, endDate);
        Map<LocalDate, BigDecimal> qcMap = aggregateQc(startDate, endDate);

        List<BossCostTrendVO> result = new ArrayList<>();
        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            BigDecimal ads = adsMap.getOrDefault(cursor, BigDecimal.ZERO);
            BigDecimal qc = qcMap.getOrDefault(cursor, BigDecimal.ZERO);
            BossCostTrendVO vo = new BossCostTrendVO();
            vo.setStatDate(cursor);
            vo.setAdsCost(ads);
            vo.setQcCost(qc);
            vo.setTotalCost(ads.add(qc));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<InactiveCompanyVO> listInactiveCompanies(int days) {
        int period = days > 0 ? days : 30;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period - 1);

        List<QcOeCompany> companies = companyService.list();
        if (CollectionUtils.isEmpty(companies)) {
            return new ArrayList<>();
        }

        Map<Long, BigDecimal> adsCost = aggregateAdsByCompany(startDate, endDate);
        Map<Long, BigDecimal> qcCost = aggregateQcByCompany(startDate, endDate);
        Map<Long, LocalDate> lastConsumeDate = aggregateLastDateByCompany(startDate, endDate);
        Map<Long, InactiveCompanyVO.SaleInfo> saleInfoMap = resolveMainSaleByCompany();

        List<InactiveCompanyVO> result = new ArrayList<>();
        for (QcOeCompany company : companies) {
            Long companyId = company.getAdvCompanyId();
            if (companyId == null) {
                continue;
            }
            BigDecimal ads = adsCost.getOrDefault(companyId, BigDecimal.ZERO);
            BigDecimal qc = qcCost.getOrDefault(companyId, BigDecimal.ZERO);
            BigDecimal total = ads.add(qc);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                continue;
            }
            InactiveCompanyVO vo = new InactiveCompanyVO();
            vo.setAdvCompanyId(companyId);
            vo.setAdvCompanyName(company.getAdvCompanyName());
            InactiveCompanyVO.SaleInfo saleInfo = saleInfoMap.get(companyId);
            if (saleInfo != null) {
                vo.setSaleUserId(saleInfo.getSaleUserId());
                vo.setSaleUserName(saleInfo.getSaleUserName());
            }
            vo.setLastConsumeDate(lastConsumeDate.get(companyId));
            result.add(vo);
        }
        return result;
    }

    private Map<LocalDate, BigDecimal> aggregateAds(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = adsDailyMapper.selectMaps(
                new QueryWrapper<QcOeAdsAdvertiserDaily>()
                        .select("stat_date as statDate", "COALESCE(sum(stat_cost),0) as totalCost")
                        .between("stat_date", Date.valueOf(startDate), Date.valueOf(endDate))
                        .groupBy("stat_date")
        );
        return toDateCostMap(rows, "statDate", "totalCost");
    }

    private Map<LocalDate, BigDecimal> aggregateQc(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = qcDailyMapper.selectMaps(
                new QueryWrapper<QcOeQcAdvertiserDaily>()
                        .select("stat_date as statDate", "COALESCE(sum(stat_cost),0) as totalCost")
                        .between("stat_date", Date.valueOf(startDate), Date.valueOf(endDate))
                        .groupBy("stat_date")
        );
        return toDateCostMap(rows, "statDate", "totalCost");
    }

    private Map<Long, BigDecimal> aggregateAdsByCompany(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = adsDailyMapper.selectCostByCompany(Date.valueOf(startDate), Date.valueOf(endDate));
        return toCompanyCostMap(rows, "advCompanyId", "totalCost");
    }

    private Map<Long, BigDecimal> aggregateQcByCompany(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rows = qcDailyMapper.selectCostByCompany(Date.valueOf(startDate), Date.valueOf(endDate));
        return toCompanyCostMap(rows, "advCompanyId", "totalCost");
    }

    private Map<Long, LocalDate> aggregateLastDateByCompany(LocalDate startDate, LocalDate endDate) {
        Map<Long, LocalDate> map = new HashMap<>();
        List<Map<String, Object>> ads = adsDailyMapper.selectLastDateByCompany(Date.valueOf(startDate), Date.valueOf(endDate));
        List<Map<String, Object>> qc = qcDailyMapper.selectLastDateByCompany(Date.valueOf(startDate), Date.valueOf(endDate));
        mergeLastDate(map, ads, "advCompanyId", "lastDate");
        mergeLastDate(map, qc, "advCompanyId", "lastDate");
        return map;
    }

    private Map<Long, InactiveCompanyVO.SaleInfo> resolveMainSaleByCompany() {
        List<Map<String, Object>> rows = advertiserMapper.selectMaps(
                new QueryWrapper<com.kakarote.admin.oceanengine.entity.QcOeAdvertiser>()
                        .select("adv_company_id as advCompanyId", "sale_user_id as saleUserId", "sale_name as saleUserName", "COUNT(*) as cnt")
                        .isNotNull("adv_company_id")
                        .gt("adv_company_id", 0)
                        .isNotNull("sale_user_id")
                        .gt("sale_user_id", 0)
                        .eq("is_deleted", 0)
                        .groupBy("adv_company_id", "sale_user_id", "sale_name")
        );
        Map<Long, InactiveCompanyVO.SaleInfo> map = new HashMap<>();
        Map<Long, Long> maxCnt = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long companyId = toLong(row.get("advCompanyId"));
            Long saleUserId = toLong(row.get("saleUserId"));
            String saleUserName = Objects.toString(row.get("saleUserName"), null);
            Long cnt = toLong(row.get("cnt"));
            if (companyId == null || saleUserId == null || cnt == null) {
                continue;
            }
            if (cnt < 0) {
                continue;
            }
            Long currentMax = maxCnt.getOrDefault(companyId, -1L);
            if (cnt > currentMax) {
                maxCnt.put(companyId, cnt);
                InactiveCompanyVO.SaleInfo info = new InactiveCompanyVO.SaleInfo();
                info.setSaleUserId(saleUserId);
                info.setSaleUserName(saleUserName);
                map.put(companyId, info);
            }
        }
        return map;
    }

    private void mergeLastDate(Map<Long, LocalDate> target, List<Map<String, Object>> rows, String companyKey, String dateKey) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Long companyId = toLong(row.get(companyKey));
            LocalDate date = toLocalDate(row.get(dateKey));
            if (companyId == null || date == null) {
                continue;
            }
            LocalDate current = target.get(companyId);
            if (current == null || date.isAfter(current)) {
                target.put(companyId, date);
            }
        }
    }

    private Map<LocalDate, BigDecimal> toDateCostMap(List<Map<String, Object>> rows, String dateKey, String costKey) {
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        if (CollectionUtils.isEmpty(rows)) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object dateVal = row.get(dateKey);
            Object costVal = row.get(costKey);
            if (dateVal == null || costVal == null) {
                continue;
            }
            LocalDate date = dateVal instanceof java.sql.Date
                    ? ((java.sql.Date) dateVal).toLocalDate()
                    : LocalDate.parse(dateVal.toString());
            BigDecimal cost = toBigDecimal(costVal);
            if (cost == null) {
                continue;
            }
            map.put(date, cost);
        }
        return map;
    }

    private Map<Long, BigDecimal> toCompanyCostMap(List<Map<String, Object>> rows, String companyKey, String costKey) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (CollectionUtils.isEmpty(rows)) {
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

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLong(Object val) {
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate toLocalDate(Object val) {
        if (val instanceof java.sql.Date) {
            return ((java.sql.Date) val).toLocalDate();
        }
        if (val instanceof java.util.Date) {
            return ((java.util.Date) val).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        try {
            return LocalDate.parse(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
