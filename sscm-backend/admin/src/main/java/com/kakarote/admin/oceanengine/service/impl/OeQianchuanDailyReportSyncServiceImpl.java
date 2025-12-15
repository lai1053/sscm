package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.admin.config.OceanEngineProperties;
import com.kakarote.admin.oceanengine.config.OceanEngineConstants;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.entity.QcOeQcAdvertiserDaily;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeQcAdvertiserDailyMapper;
import com.kakarote.admin.oceanengine.service.OceanTokenService;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.OeQianchuanDailyReportSyncService.QcDailySyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 千川日报同步
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeQianchuanDailyReportSyncServiceImpl implements OeQianchuanDailyReportSyncService {

    private static final String QC_REPORT_PATH = OceanEngineConstants.QC_ADVERTISER_REPORT_PATH;

    private final QcOeAdvertiserMapper advertiserMapper;
    private final QcOeQcAdvertiserDailyMapper dailyMapper;
    private final OceanTokenService tokenService;
    private final RestTemplate restTemplate;
    private final OceanEngineProperties oceanEngineProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QcDailySyncResult syncQcDaily(LocalDate statDate) {
        QcDailySyncResult result = new QcDailySyncResult();
        if (statDate == null) {
            log.warn("[QC_DAILY] statDate is null, skip");
            return result;
        }
        List<QcOeAdvertiser> advertisers = advertiserMapper.selectList(
                new LambdaQueryWrapper<QcOeAdvertiser>()
                        .eq(QcOeAdvertiser::getChannel, OceanEngineConstants.CHANNEL_QIANCHUAN)
                        .eq(QcOeAdvertiser::getIsDeleted, 0)
                        .select(QcOeAdvertiser::getId, QcOeAdvertiser::getAdvertiserId)
        );
        List<Long> advertiserIds = CollectionUtils.isEmpty(advertisers)
                ? Collections.emptyList()
                : advertisers.stream().map(QcOeAdvertiser::getAdvertiserId).collect(Collectors.toList());
        return syncQcDaily(statDate, statDate, advertiserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QcDailySyncResult syncQcDaily(LocalDate startDate, LocalDate endDate, List<Long> advertiserIds) {
        QcDailySyncResult result = new QcDailySyncResult();
        if (startDate == null || endDate == null) {
            log.warn("[QC_DAILY] start/end date is null, skip");
            return result;
        }
        if (CollectionUtils.isEmpty(advertiserIds)) {
            log.info("[QC_DAILY] empty advertiserIds");
            return result;
        }
        int total = 0;
        int success = 0;
        int fail = 0;
        for (Long advertiserId : advertiserIds) {
            if (advertiserId == null) {
                continue;
            }
            LocalDate windowStart = startDate;
            while (!windowStart.isAfter(endDate)) {
                LocalDate windowEnd = windowStart.plusDays(29);
                if (windowEnd.isAfter(endDate)) {
                    windowEnd = endDate;
                }
                total++;
                try {
                    boolean ok = fetchAndUpsertMetricsRange(windowStart, windowEnd, advertiserId);
                    if (ok) {
                        success++;
                    } else {
                        fail++;
                    }
                } catch (Exception e) {
                    fail++;
                    log.warn("[QC_DAILY] sync failed, advertiserId={}, range={}~{}, msg={}", advertiserId, windowStart, windowEnd, e.getMessage(), e);
                }
                windowStart = windowEnd.plusDays(1);
            }
        }
        result.setTotal(total);
        result.setSuccess(success);
        result.setFail(fail);
        log.info("[QC_DAILY] sync finished, start={}, end={}, totalRecords={}, success={}, fail={}",
                startDate, endDate, total, success, fail);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QcDailySyncResult syncQcDailyForAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate) {
        return syncQcDaily(startDate, endDate, Collections.singletonList(advertiserId));
    }

    @Override
    public List<Long> listQcAdvertiserIdsByCompany(Long advCompanyId) {
        if (advCompanyId == null) {
            return Collections.emptyList();
        }
        List<QcOeAdvertiser> advertisers = advertiserMapper.selectList(
                new LambdaQueryWrapper<QcOeAdvertiser>()
                        .eq(QcOeAdvertiser::getChannel, OceanEngineConstants.CHANNEL_QIANCHUAN)
                        .eq(QcOeAdvertiser::getIsDeleted, 0)
                        .eq(QcOeAdvertiser::getAdvCompanyId, advCompanyId)
                        .select(QcOeAdvertiser::getAdvertiserId)
        );
        if (CollectionUtils.isEmpty(advertisers)) {
            return Collections.emptyList();
        }
        return advertisers.stream()
                .map(QcOeAdvertiser::getAdvertiserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QcDailySyncResult syncByCompany(Long advCompanyId, int days) {
        int period = days <= 0 ? 7 : days;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period - 1L);
        List<Long> advertiserIds = listQcAdvertiserIdsByCompany(advCompanyId);
        if (CollectionUtils.isEmpty(advertiserIds)) {
            log.info("[QC_DAILY] no advertisers for company {}", advCompanyId);
            return new QcDailySyncResult();
        }
        return syncQcDaily(startDate, endDate, advertiserIds);
    }

    private boolean fetchAndUpsertMetrics(LocalDate statDate, Long advertiserId) {
        return fetchAndUpsertMetricsRange(statDate, statDate, advertiserId);
    }

    private boolean fetchAndUpsertMetricsRange(LocalDate startDate, LocalDate endDate, Long advertiserId) {
        String accessToken = tokenService.getAccessToken(OceanChannelCode.OCEANENGINE_QIANCHUAN);
        URI uri;
        try {
            uri = buildRequestUri(startDate, endDate, advertiserId);
        } catch (Exception e) {
            log.warn("[QC_DAILY] build request url fail, advertiserId={}, range={}~{}, msg={}", advertiserId, startDate, endDate, e.getMessage(), e);
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Token", accessToken);

        if (log.isDebugEnabled()) {
            log.debug("[QC_DAILY] request url={}, method=GET, headers={}", uri, headers);
        }

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            String respBody = resp.getBody();
            if (log.isDebugEnabled()) {
                log.debug("[QC_DAILY] response status={}, body={}", resp.getStatusCode(), respBody);
            }
            QcReportResponse dto = objectMapper.readValue(respBody, QcReportResponse.class);
            if (dto == null || dto.getCode() == null) {
                log.warn("[QC_DAILY] parse resp null, advertiserId={}, range={}~{}, body={}", advertiserId, startDate, endDate, respBody);
                return false;
            }
            if (!Objects.equals(dto.getCode(), 0)) {
                log.warn("[QC_DAILY] api returned non-zero code, advertiserId={}, range={}~{}, code={}, message={}, helpMessage={}, request_id={}",
                        advertiserId, startDate, endDate, dto.getCode(), dto.getMessage(), dto.getHelpMessage(), dto.getRequestId());
                return false;
            }
            if (dto.getData() == null || CollectionUtils.isEmpty(dto.getData().getList())) {
                log.info("[QC_DAILY] no data, advertiserId={}, range={}~{}, body={}", advertiserId, startDate, endDate, respBody);
                return true;
            }
            java.util.Date now = new java.util.Date();
            java.util.List<QcOeQcAdvertiserDaily> batch = new java.util.ArrayList<>();
            for (QcReportRow row : dto.getData().getList()) {
                LocalDate rowDate = parseRowDate(row, startDate);
                if (rowDate == null) {
                    continue;
                }
                QcMetrics metrics = new QcMetrics();
                metrics.setStatCost(parseDecimal(row.getStatCost()));
                metrics.setShowCnt(parseLong(row.getShowCnt()));
                metrics.setClickCnt(parseLong(row.getClickCnt()));
                metrics.setPayOrderCount(parseLong(row.getPayOrderCount()));
                metrics.setPayGmv(parseDecimal(firstNonBlank(row.getPayOrderAmount(), row.getPayGmv())));
                metrics.setRoi(parseDecimal(row.getRoi()));
                QcOeQcAdvertiserDaily daily = new QcOeQcAdvertiserDaily();
                daily.setAdvertiserId(advertiserId);
                daily.setStatDate(java.sql.Date.valueOf(rowDate));
                daily.setChannel("QIANCHUAN");
                daily.setStatCost(metrics.getStatCost());
                daily.setShowCnt(metrics.getShowCnt());
                daily.setClickCnt(metrics.getClickCnt());
                daily.setPayOrderCount(metrics.getPayOrderCount());
                daily.setPayGmv(metrics.getPayGmv());
                daily.setRoi(metrics.getRoi());
                daily.setGmtCreate(now);
                daily.setGmtModified(now);
                batch.add(daily);
                updateLastCostDateIfNewer(advertiserId, rowDate, metrics.getStatCost());
            }
            if (!batch.isEmpty()) {
                dailyMapper.upsertBatch(batch);
            }
            return true;
        } catch (HttpClientErrorException e) {
            log.warn("[QC_DAILY] api error advertiserId={}, range={}~{}, status={}, body={}",
                    advertiserId, startDate, endDate, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.warn("[QC_DAILY] request fail advertiserId={}, range={}~{}, msg={}", advertiserId, startDate, endDate, e.getMessage(), e);
            return false;
        }
    }

    private void upsertDaily(Long advertiserId, LocalDate statDate, QcMetrics metrics) {
        Date statDateVal = Date.valueOf(statDate);
        QcOeQcAdvertiserDaily existing = dailyMapper.selectOne(
                new LambdaQueryWrapper<QcOeQcAdvertiserDaily>()
                        .eq(QcOeQcAdvertiserDaily::getAdvertiserId, advertiserId)
                        .eq(QcOeQcAdvertiserDaily::getStatDate, statDateVal)
                        .last("limit 1")
        );
        java.util.Date now = new java.util.Date();
        if (existing == null) {
            QcOeQcAdvertiserDaily daily = new QcOeQcAdvertiserDaily();
            daily.setAdvertiserId(advertiserId);
            daily.setStatDate(statDateVal);
            daily.setChannel(OceanEngineConstants.CHANNEL_QIANCHUAN);
            daily.setStatCost(metrics.getStatCost());
            daily.setShowCnt(metrics.getShowCnt());
            daily.setClickCnt(metrics.getClickCnt());
            daily.setPayOrderCount(metrics.getPayOrderCount());
            daily.setPayGmv(metrics.getPayGmv());
            daily.setRoi(metrics.getRoi());
            daily.setIsDeleted(0);
            daily.setGmtCreate(now);
            daily.setGmtModified(now);
            dailyMapper.insert(daily);
            updateLastCostDateIfNewer(advertiserId, statDate, metrics.getStatCost());
        } else {
            existing.setStatCost(metrics.getStatCost());
            existing.setShowCnt(metrics.getShowCnt());
            existing.setClickCnt(metrics.getClickCnt());
            existing.setPayOrderCount(metrics.getPayOrderCount());
            existing.setPayGmv(metrics.getPayGmv());
            existing.setRoi(metrics.getRoi());
            if (existing.getIsDeleted() == null) {
                existing.setIsDeleted(0);
            }
            existing.setGmtModified(now);
            dailyMapper.updateById(existing);
            updateLastCostDateIfNewer(advertiserId, statDate, metrics.getStatCost());
        }
    }

    private BigDecimal parseDecimal(String val) {
        if (val == null) {
            return null;
        }
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return null;
        }
    }

    private void updateLastCostDateIfNewer(Long advertiserId, LocalDate statDate, BigDecimal statCost) {
        if (advertiserId == null || statDate == null || statCost == null) {
            return;
        }
        if (statCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            advertiserMapper.updateLastCostDateIfNewer(advertiserId, java.sql.Date.valueOf(statDate));
        } catch (Exception e) {
            log.warn("[QC_DAILY] update last_cost_date fail advertiserId={}, statDate={}, msg={}", advertiserId, statDate, e.getMessage(), e);
        }
    }

    private Long parseLong(String val) {
        if (val == null) {
            return null;
        }
        try {
            return Long.valueOf(val);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String val) {
        if (val == null) {
            return null;
        }
        String trimmed = val;
        if (val.length() >= 10) {
            trimmed = val.substring(0, 10);
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseRowDate(QcReportRow row, LocalDate defaultDate) {
        String candidate = firstNonBlank(row.getStatTimeDay(), row.getStatTime(), row.getStatDatetime());
        LocalDate date = parseDate(candidate);
        return date == null ? defaultDate : date;
    }

    private String firstNonBlank(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private URI buildRequestUri(LocalDate statDate, Long advertiserId) throws Exception {
        return buildRequestUri(statDate, statDate, advertiserId);
    }

    private URI buildRequestUri(LocalDate startDate, LocalDate endDate, Long advertiserId) throws Exception {
        String baseUrl = oceanEngineProperties.getQcBaseUrl();
        Map<String, Object> filtering = new LinkedHashMap<>();
        filtering.put("marketing_goal", "ALL");
        String fieldsJson = objectMapper.writeValueAsString(Arrays.asList(
                "stat_cost", "show_cnt", "click_cnt",
                "pay_order_count", "pay_order_amount"
        ));
        String filteringJson = objectMapper.writeValueAsString(filtering);

        return UriComponentsBuilder.fromHttpUrl(baseUrl + QC_REPORT_PATH)
                .queryParam("advertiser_id", advertiserId)
                .queryParam("start_date", startDate.toString())
                .queryParam("end_date", endDate.toString())
                .queryParam("time_granularity", "TIME_GRANULARITY_DAILY")
                .queryParam("fields", fieldsJson)
                .queryParam("filtering", filteringJson)
                .queryParam("order_field", "stat_cost")
                .queryParam("order_type", "DESC")
                .queryParam("page", 1)
                .queryParam("page_size", 100)
                .build()
                .toUri();
    }

    @lombok.Data
    private static class QcMetrics {
        private BigDecimal statCost;
        private Long showCnt;
        private Long clickCnt;
        private Long payOrderCount;
        private BigDecimal payGmv;
        private BigDecimal roi;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QcReportResponse {
        private Integer code;
        private String message;
        @JsonProperty("request_id")
        private String requestId;
        @JsonProperty("help_message")
        private String helpMessage;
        private QcReportData data;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QcReportData {
        @JsonProperty("page_info")
        private QcPageInfo pageInfo;
        @JsonProperty("list")
        private List<QcReportRow> list;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QcPageInfo {
        @JsonProperty("page")
        private Integer page;
        @JsonProperty("page_size")
        private Integer pageSize;
        @JsonProperty("total_page")
        private Integer totalPage;
        @JsonProperty("total_number")
        private Integer totalNumber;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QcReportRow {
        @JsonProperty("stat_time_day")
        private String statTimeDay;
        @JsonProperty("stat_time")
        private String statTime;
        @JsonProperty("stat_datetime")
        private String statDatetime;
        @JsonProperty("stat_cost")
        private String statCost;
        @JsonProperty("show_cnt")
        private String showCnt;
        @JsonProperty("click_cnt")
        private String clickCnt;
        @JsonProperty("pay_order_count")
        private String payOrderCount;
        @JsonProperty("pay_order_amount")
        private String payOrderAmount;
        @JsonProperty("pay_gmv")
        private String payGmv;
        @JsonProperty("roi")
        private String roi;
    }
}
