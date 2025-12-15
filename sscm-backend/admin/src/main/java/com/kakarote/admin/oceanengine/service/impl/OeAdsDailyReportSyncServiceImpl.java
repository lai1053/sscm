package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.admin.config.OceanEngineProperties;
import com.kakarote.admin.oceanengine.config.OceanEngineConstants;
import com.kakarote.admin.oceanengine.entity.QcOeAdsAdvertiserDaily;
import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.mapper.QcOeAdsAdvertiserDailyMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.service.OceanTokenService;
import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService;
import com.kakarote.admin.oceanengine.service.OeAdsDailyReportSyncService.AdsDailySyncResult;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * ADS日报同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeAdsDailyReportSyncServiceImpl implements OeAdsDailyReportSyncService {

    private static final String ADS_REPORT_PATH = OceanEngineConstants.ADS_CUSTOM_REPORT_V3_PATH;

    private final QcOeAdvertiserMapper advertiserMapper;
    private final QcOeAdsAdvertiserDailyMapper dailyMapper;
    private final OceanTokenService tokenService;
    private final RestTemplate restTemplate;
    private final OceanEngineProperties oceanEngineProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdsDailySyncResult syncAdsDaily(LocalDate statDate) {
        if (statDate == null) {
            log.warn("[ADS_DAILY] statDate is null, skip");
            return new AdsDailySyncResult();
        }
        List<QcOeAdvertiser> advertisers = advertiserMapper.selectList(
                new LambdaQueryWrapper<QcOeAdvertiser>()
                        .eq(QcOeAdvertiser::getChannel, OceanEngineConstants.CHANNEL_ADS)
                        .eq(QcOeAdvertiser::getIsDeleted, 0)
                        .select(QcOeAdvertiser::getId, QcOeAdvertiser::getAdvertiserId)
        );
        List<Long> advertiserIds = CollectionUtils.isEmpty(advertisers) ? Collections.emptyList() :
                advertisers.stream().map(QcOeAdvertiser::getAdvertiserId).collect(Collectors.toList());
        return syncAdsDaily(statDate, statDate, advertiserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdsDailySyncResult syncAdsDaily(LocalDate startDate, LocalDate endDate, List<Long> advertiserIds) {
        AdsDailySyncResult result = new AdsDailySyncResult();
        if (startDate == null || endDate == null) {
            log.warn("[ADS_DAILY] start/end date is null, skip");
            return result;
        }
        if (CollectionUtils.isEmpty(advertiserIds)) {
            log.info("[ADS_DAILY] empty advertiserIds");
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
                LocalDate windowEnd = windowStart.plusDays(89);
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
                    log.warn("[ADS_DAILY] sync failed, advertiserId={}, range={}~{}, msg={}",
                            advertiserId, windowStart, windowEnd, e.getMessage(), e);
                }
                windowStart = windowEnd.plusDays(1);
            }
        }
        result.setTotal(total);
        result.setSuccess(success);
        result.setFail(fail);
        log.info("[ADS_DAILY] sync finished, start={}, end={}, totalRequests={}, success={}, fail={}",
                startDate, endDate, total, success, fail);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdsDailySyncResult syncAdsDailyForAdvertiser(Long advertiserId, LocalDate startDate, LocalDate endDate) {
        return syncAdsDaily(startDate, endDate, Collections.singletonList(advertiserId));
    }

    @Override
    public List<Long> listAdsAdvertiserIdsByCompany(Long advCompanyId) {
        if (advCompanyId == null) {
            return Collections.emptyList();
        }
        List<QcOeAdvertiser> advertisers = advertiserMapper.selectList(
                new LambdaQueryWrapper<QcOeAdvertiser>()
                        .eq(QcOeAdvertiser::getChannel, OceanEngineConstants.CHANNEL_ADS)
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

    private boolean fetchAndUpsertMetrics(LocalDate statDate, Long advertiserId) {
        return fetchAndUpsertMetricsRange(statDate, statDate, advertiserId);
    }

    private boolean fetchAndUpsertMetricsRange(LocalDate startDate, LocalDate endDate, Long advertiserId) {
        String accessToken = tokenService.getAccessToken(OceanChannelCode.OCEANENGINE_ADS);
        URI uri;
        try {
            uri = buildRequestUri(startDate, endDate, advertiserId);
        } catch (Exception e) {
            log.warn("[ADS_DAILY] build request url fail, advertiserId={}, range={}~{}, msg={}", advertiserId, startDate, endDate, e.getMessage(), e);
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Token", accessToken);

        if (log.isDebugEnabled()) {
            log.debug("[ADS_DAILY] request url={}, method=GET, headers={}", uri, headers);
        }

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            String respBody = resp.getBody();
            if (log.isDebugEnabled()) {
                log.debug("[ADS_DAILY] response status={}, body={}", resp.getStatusCode(), respBody);
            }
            OeReportV3Response dto = objectMapper.readValue(respBody, OeReportV3Response.class);
            if (dto == null || dto.getCode() == null) {
                log.warn("[ADS_DAILY] parse resp null, advertiserId={}, range={}~{}, body={}", advertiserId, startDate, endDate, respBody);
                return false;
            }
            if (!Objects.equals(dto.getCode(), 0)) {
                log.warn("[ADS_DAILY] api returned non-zero code, advertiserId={}, range={}~{}, code={}, message={}, helpMessage={}, request_id={}",
                        advertiserId, startDate, endDate, dto.getCode(), dto.getMessage(), dto.getHelpMessage(), dto.getRequestId());
                return false;
            }
            if (dto.getData() == null || CollectionUtils.isEmpty(dto.getData().getRows())) {
                log.info("[ADS_DAILY] no data, advertiserId={}, range={}~{}, body={}", advertiserId, startDate, endDate, respBody);
                return true;
            }
            java.util.Date now = new java.util.Date();
            java.sql.Date sqlNow = new java.sql.Date(now.getTime());
            java.util.List<QcOeAdsAdvertiserDaily> batch = new java.util.ArrayList<>();
            for (ReportRow row : dto.getData().getRows()) {
                LocalDate rowDate = parseDate(row.getDimensions().get("stat_time_day"));
                if (rowDate == null) {
                    continue;
                }
                DailyMetrics metrics = new DailyMetrics();
                metrics.setStatCost(parseDecimal(row.getMetrics().get("stat_cost")));
                metrics.setShowCnt(parseLong(row.getMetrics().get("show_cnt")));
                metrics.setClickCnt(parseLong(row.getMetrics().get("click_cnt")));
                QcOeAdsAdvertiserDaily daily = new QcOeAdsAdvertiserDaily();
                daily.setAdvertiserId(advertiserId);
                daily.setStatDate(java.sql.Date.valueOf(rowDate));
                daily.setChannel("ADS");
                daily.setStatCost(metrics.getStatCost());
                daily.setShowCnt(metrics.getShowCnt());
                daily.setClickCnt(metrics.getClickCnt());
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
            log.warn("[ADS_DAILY] api error advertiserId={}, range={}~{}, status={}, body={}",
                    advertiserId, startDate, endDate, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.warn("[ADS_DAILY] request fail advertiserId={}, range={}~{}, msg={}", advertiserId, startDate, endDate, e.getMessage(), e);
            return false;
        }
    }

    private void upsertDaily(Long advertiserId, LocalDate statDate, DailyMetrics metrics) {
        Date statDateVal = Date.valueOf(statDate);
        QcOeAdsAdvertiserDaily existing = dailyMapper.selectOne(
                new LambdaQueryWrapper<QcOeAdsAdvertiserDaily>()
                        .eq(QcOeAdsAdvertiserDaily::getAdvertiserId, advertiserId)
                        .eq(QcOeAdsAdvertiserDaily::getStatDate, statDateVal)
                        .last("limit 1")
        );
        java.util.Date now = new java.util.Date();
        if (existing == null) {
            QcOeAdsAdvertiserDaily daily = new QcOeAdsAdvertiserDaily();
            daily.setAdvertiserId(advertiserId);
            daily.setStatDate(statDateVal);
            daily.setChannel(OceanEngineConstants.CHANNEL_ADS);
            daily.setStatCost(metrics.getStatCost());
            daily.setShowCnt(metrics.getShowCnt());
            daily.setClickCnt(metrics.getClickCnt());
            daily.setGmtCreate(now);
            daily.setGmtModified(now);
            dailyMapper.insert(daily);
            updateLastCostDateIfNewer(advertiserId, statDate, metrics.getStatCost());
        } else {
            existing.setStatCost(metrics.getStatCost());
            existing.setShowCnt(metrics.getShowCnt());
            existing.setClickCnt(metrics.getClickCnt());
            existing.setGmtModified(now);
            dailyMapper.updateById(existing);
            updateLastCostDateIfNewer(advertiserId, statDate, metrics.getStatCost());
        }
    }

    private BigDecimal getDecimal(JsonNode node, String field) {
        JsonNode val = node.get(field);
        if (val == null || !val.isNumber()) {
            return null;
        }
        return val.decimalValue();
    }

    private Long getLong(JsonNode node, String field) {
        JsonNode val = node.get(field);
        if (val == null || !val.canConvertToLong()) {
            return null;
        }
        return val.asLong();
    }

    private LocalDate parseDate(String val) {
        try {
            return LocalDate.parse(val);
        } catch (Exception e) {
            log.warn("[ADS_DAILY] parse date fail val={}", val);
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
            log.warn("[ADS_DAILY] update last_cost_date fail advertiserId={}, statDate={}, msg={}", advertiserId, statDate, e.getMessage(), e);
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

    private URI buildRequestUri(LocalDate statDate, Long advertiserId) throws Exception {
        return buildRequestUri(statDate, statDate, advertiserId);
    }

    private URI buildRequestUri(LocalDate startDate, LocalDate endDate, Long advertiserId) throws Exception {
        String baseUrl = oceanEngineProperties.getAdsBaseUrl();
        List<String> dimensions = Collections.singletonList("stat_time_day");
        List<String> metrics = Arrays.asList("stat_cost", "show_cnt", "click_cnt");
        String dimensionsJson = objectMapper.writeValueAsString(dimensions);
        String metricsJson = objectMapper.writeValueAsString(metrics);
        String filtersJson = objectMapper.writeValueAsString(Collections.emptyList());
        String orderByJson = objectMapper.writeValueAsString(Collections.emptyList());

        return UriComponentsBuilder.fromHttpUrl(baseUrl + ADS_REPORT_PATH)
                .queryParam("advertiser_id", advertiserId)
                .queryParam("data_topic", "BASIC_DATA")
                .queryParam("dimensions", dimensionsJson)
                .queryParam("metrics", metricsJson)
                .queryParam("filters", filtersJson)
                .queryParam("order_by", orderByJson)
                .queryParam("start_time", startDate.toString())
                .queryParam("end_time", endDate.toString())
                .build()
                .toUri();
    }

    @lombok.Data
    private static class DailyMetrics {
        private BigDecimal statCost;
        private Long showCnt;
        private Long clickCnt;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OeReportV3Response {
        private Integer code;
        private String message;
        @JsonProperty("request_id")
        private String requestId;
        @JsonProperty("help_message")
        private String helpMessage;
        private ReportData data;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ReportData {
        @JsonProperty("page_info")
        private PageInfo pageInfo;
        private List<ReportRow> rows;
        @JsonProperty("total_metrics")
        private Map<String, String> totalMetrics;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PageInfo {
        @JsonProperty("page")
        private Integer page;
        @JsonProperty("page_size")
        private Integer pageSize;
        @JsonProperty("total_number")
        private Integer totalNumber;
        @JsonProperty("total_page")
        private Integer totalPage;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ReportRow {
        @JsonProperty("dimensions")
        private Map<String, String> dimensions;
        @JsonProperty("metrics")
        private Map<String, String> metrics;
    }
}
