package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.bytedance.ads.ApiClient;
import com.bytedance.ads.ApiException;
import com.bytedance.ads.api.AgentAdvertiserInfoQueryV2Api;
import com.bytedance.ads.api.AgentAdvertiserSelectV2Api;
import com.bytedance.ads.model.AgentAdvertiserInfoQueryV2Response;
import com.bytedance.ads.model.AgentAdvertiserInfoQueryV2ResponseData;
import com.bytedance.ads.model.AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner;
import com.bytedance.ads.model.AgentAdvertiserSelectV2Response;
import com.bytedance.ads.model.AgentAdvertiserSelectV2ResponseData;
import com.bytedance.ads.model.AgentAdvertiserSelectV2ResponseDataCursorPageInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.kakarote.admin.oceanengine.client.OceanEngineOpenApiClient;
import com.kakarote.admin.config.OceanEngineProperties;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.service.OceanTokenService;
import com.kakarote.admin.oceanengine.service.OeAccountDiscoveryService;
import com.kakarote.admin.oceanengine.service.OeAgentAdvertiserRelationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 账户发现：调用巨量接口拉取代理商与广告主基础信息，统一转换为 DTO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OeAccountDiscoveryServiceImpl implements OeAccountDiscoveryService {

    // agent/advertiser_info/query 单次最多允许 50 个 advertiser_id，超过会返回 40001
    private static final int INFO_BATCH_SIZE = 50;

    private final OceanTokenService tokenService;
    private final OceanEngineOpenApiClient openApiClient;
    private final AgentAdvertiserSelectV2Api advertiserSelectApi;
    private final AgentAdvertiserInfoQueryV2Api advertiserInfoApi;
    private final ApiClient oceanEngineApiClient;
    private final OceanEngineProperties properties;
    private final OeAgentAdvertiserRelationService relationService;

    @Override
    public List<AdvertiserDetail> discoverAdvertisers(String channelCode) {
        OceanChannelCode channel = OceanChannelCode.fromCode(channelCode);
        // 每次调用前动态写入 token，避免复用过期 token
        String accessToken = tokenService.getAccessToken(channel);
        oceanEngineApiClient.addDefaultHeader("Access-Token", accessToken);

        try {
            if (channel == OceanChannelCode.OCEANENGINE_ADS) {
                return discoverByAdsChannel(channel);
            }
            return discoverByQianchuanChannel(channel);
        } catch (ApiException ex) {
            log.error("[OE_DISCOVERY] 调用巨量接口失败, channel={}, msg={}", channel.getCode(), ex.getMessage(), ex);
            throw new RuntimeException("调用巨量接口失败: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("[OE_DISCOVERY] 账户发现异常, channel={}, msg={}", channel.getCode(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private List<AdvertiserDetail> discoverByAdsChannel(OceanChannelCode channel) throws ApiException {
        // 1) oauth2/advertiser/get 返回的 advertiser_id 在 ADS 场景代表代理商账户
        List<Long> agentIds = parseAdvertiserIdsFromOauth(channel);
        if (CollectionUtils.isEmpty(agentIds)) {
            log.warn("[OE_DISCOVERY] ADS 渠道通过 oauth2/advertiser/get 未发现任何 agentId");
            return new ArrayList<>();
        }
        // 2) 针对每个 agentId 调用 select 拉取广告主列表
        List<Long> advertiserIds = fetchAdvertiserIdsByAgents(agentIds);
        if (CollectionUtils.isEmpty(advertiserIds)) {
            log.warn("[OE_DISCOVERY] ADS 渠道未发现广告主");
            return new ArrayList<>();
        }
        return fetchAdvertiserDetails(advertiserIds, channel.getCode(), "ADS");
    }

    private List<AdvertiserDetail> discoverByQianchuanChannel(OceanChannelCode channel) throws ApiException {
        // 1) oauth2/advertiser/get 返回千川代理商账号（agentIds）
        List<Long> agentIds = parseAdvertiserIdsFromOauth(channel);
        if (CollectionUtils.isEmpty(agentIds)) {
            log.warn("[OE_DISCOVERY] QIANCHUAN 渠道通过 oauth2/advertiser/get 未发现任何 agentId");
            return new ArrayList<>();
        }
        // 2) 针对每个 agentId 调用 select 拉取广告主列表
        List<Long> advertiserIds = fetchAdvertiserIdsByAgents(agentIds);
        if (CollectionUtils.isEmpty(advertiserIds)) {
            log.warn("[OE_DISCOVERY] QIANCHUAN 渠道未发现广告主");
            return new ArrayList<>();
        }
        return fetchAdvertiserDetails(advertiserIds, channel.getCode(), "QIANCHUAN");
    }

    /**
     * 调用 select 接口分页获取 advertiserIds
     */
    @Deprecated
    private List<Long> fetchAdvertiserIds(String accessToken) throws ApiException {
        Long agentId = resolvePrimaryAgentId(accessToken);
        if (agentId == null) {
            log.warn("[OE_DISCOVERY] 无法获取 agentId，返回空列表");
            return new ArrayList<>();
        }
        long page = 1;
        long totalPage;
        List<Long> all = new ArrayList<>();
        do {
            AgentAdvertiserSelectV2Response resp = advertiserSelectApi.openApi2AgentAdvertiserSelectGet(
                    agentId, null, null, null, null, page, 100L
            );
            AgentAdvertiserSelectV2ResponseData data = resp == null ? null : resp.getData();
            if (data == null || CollectionUtils.isEmpty(data.getList())) {
                break;
            }
            all.addAll(data.getList());
            totalPage = data.getPageInfo() == null
                    ? page
                    : Optional.ofNullable(data.getPageInfo().getTotalPage()).orElse(page);
            page++;
        } while (page <= totalPage);
        log.info("[OE_DISCOVERY] select advertisers size={}", all.size());
        return all;
    }

    /**
     * 优先取配置的 primaryAgentId；否则调用 agent/info 自动发现
     */
    private Long resolvePrimaryAgentId(String accessToken) {
        Long agentId = properties.getPrimaryAgentId();
        if (agentId != null) {
            return agentId;
        }
        try {
            String tokenParam = URLEncoder.encode(accessToken, StandardCharsets.UTF_8.name());
            String url = "https://ad.oceanengine.com/open_api/2/agent/info/?access_token=" + tokenParam;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = new RestTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            String body = resp.getBody();
            JsonNode dataNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).path("data");
            JsonNode listNode = dataNode.path("list");
            if (listNode == null || listNode.isMissingNode() || !listNode.isArray() || listNode.size() == 0) {
                log.warn("[OE_DISCOVERY] agent/info 未返回代理商信息，body={}", body);
                return null;
            }
            List<Long> agentIds = new ArrayList<>();
            listNode.forEach(node -> {
                JsonNode idNode = node.get("agent_id");
                if (idNode != null && idNode.canConvertToLong()) {
                    agentIds.add(idNode.asLong());
                }
            });
            if (CollectionUtils.isEmpty(agentIds)) {
                log.warn("[OE_DISCOVERY] agent/info 无 agent_id 字段，body={}", body);
                return null;
            }
            if (agentIds.size() > 1) {
                log.warn("[OE_DISCOVERY] agent/info 返回多个代理商，暂取第一个，ids={}", agentIds);
            }
            Long discovered = agentIds.get(0);
            log.info("[OE_DISCOVERY] 自动发现 primaryAgentId={}", discovered);
            return discovered;
        } catch (Exception e) {
            log.error("[OE_DISCOVERY] 调用 agent/info 发现 agentId 失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 针对给定代理商列表，调用 select 拉取广告主列表
     */
    /**
     * 使用 cursor+count 方式拉取广告主列表，支持超大规模列表。
     */
    private List<Long> fetchAdvertiserIdsByAgents(List<Long> agentIds) throws ApiException {
        if (CollectionUtils.isEmpty(agentIds)) {
            log.warn("[OE_DISCOVERY] ADS 渠道未配置或未发现任何 agentId");
            return new ArrayList<>();
        }
        List<Long> all = new ArrayList<>();
        for (Long agentId : agentIds) {
            Long cursor = null;
            int loop = 0;
            List<Long> agentAdvertisers = new ArrayList<>();
            String accountSource = null;
            while (true) {
                AgentAdvertiserSelectV2Response resp = advertiserSelectApi.openApi2AgentAdvertiserSelectGet(
                        agentId,
                        null,               // companyIds
                        100L,               // count
                        cursor,             // cursor
                        null,               // filtering
                        null,               // page
                        null                // pageSize
                );
                AgentAdvertiserSelectV2ResponseData data = resp == null ? null : resp.getData();
                if (data == null || CollectionUtils.isEmpty(data.getList())) {
                    log.warn("[OE_DISCOVERY] agentId={} advertiser list empty, cursor={}", agentId, cursor);
                    break;
                }
                if (accountSource == null) {
                    accountSource = data.getAccountSource();
                }
                all.addAll(data.getList());
                agentAdvertisers.addAll(data.getList());
                log.info("[OE_DISCOVERY] agentId={} cursorLoop={} cursor={} size={}",
                        agentId, ++loop, cursor, data.getList().size());

                AgentAdvertiserSelectV2ResponseDataCursorPageInfo cursorPageInfo = data.getCursorPageInfo();
                Boolean hasMore = cursorPageInfo == null ? null : cursorPageInfo.getHasMore();
                Long nextCursor = cursorPageInfo == null ? null : cursorPageInfo.getCursor();
                if (Boolean.FALSE.equals(hasMore) || nextCursor == null || nextCursor == 0L) {
                    break;
                }
                cursor = nextCursor;
            }
            if (!CollectionUtils.isEmpty(agentAdvertisers)) {
                relationService.syncRelations(agentId, "", "", accountSource, agentAdvertisers,
                        com.kakarote.admin.oceanengine.config.OceanEngineConstants.CHANNEL_ADS);
            }
        }
        log.info("[OE_DISCOVERY] ADS select advertisers size={}", all.size());
        return all;
    }

    /**
     * 依据 OAuth2 响应体解析 advertiser_id 列表
     */
    private List<Long> parseAdvertiserIdsFromOauth(OceanChannelCode channel) {
        try {
            JsonNode dataNode = openApiClient.getAdvertiserByToken(channel).path("data");
            if (dataNode == null || dataNode.isMissingNode()) {
                return new ArrayList<>();
            }
            JsonNode listNode = dataNode.path("list");
            if (listNode == null || listNode.isMissingNode()) {
                return new ArrayList<>();
            }
            return listNode.findValues("advertiser_id")
                    .stream()
                    .map(JsonNode::asLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[OE_DISCOVERY] 解析 oauth2/advertiser/get 失败, channel={}, msg={}", channel.getCode(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 调用 advertiser_info 批量获取详情并转换为 DTO
     */
    private List<AdvertiserDetail> fetchAdvertiserDetails(List<Long> advertiserIds, String channelCode, String channel) throws ApiException {
        if (CollectionUtils.isEmpty(advertiserIds)) {
            return new ArrayList<>();
        }
        if (log.isDebugEnabled()) {
            log.debug("[OE_DISCOVERY] advertiserIds size={}", advertiserIds.size());
        }
        List<AdvertiserDetail> result = new ArrayList<>();
        for (int i = 0; i < advertiserIds.size(); i += INFO_BATCH_SIZE) {
            List<Long> batch = advertiserIds.subList(i, Math.min(i + INFO_BATCH_SIZE, advertiserIds.size()));
            if (log.isDebugEnabled()) {
                log.debug("[OE_DISCOVERY] info query batch size={}, ids={}", batch.size(), batch);
            }
            try {
                List<AdvertiserDetail> batchDetails = fetchAdvertiserDetailsBatch(batch, channelCode, channel);
                if (!CollectionUtils.isEmpty(batchDetails)) {
                    log.info("[OE_DISCOVERY] info batch ok, inputSize={}, detailSize={}", batch.size(), batchDetails.size());
                    result.addAll(batchDetails);
                }
            } catch (Exception e) {
                log.warn("[OE_DISCOVERY] info batch failed, size={}, msg={}", batch.size(), e.getMessage(), e);
            }
        }
        log.info("[OE_DISCOVERY] channel={} advertiser detail size={}", channelCode, result.size());
        return result;
    }

    private List<AdvertiserDetail> fetchAdvertiserDetailsBatch(List<Long> batchIds, String channelCode, String channel) throws ApiException {
        List<AdvertiserDetail> result = new ArrayList<>();
        AgentAdvertiserInfoQueryV2Response resp = advertiserInfoApi.openApi2AgentAdvertiserInfoQueryGet(batchIds);
        if (resp == null) {
            log.warn("[OE_DISCOVERY] info batch resp null");
            return Collections.emptyList();
        }
        log.debug("[OE_DISCOVERY] info batch resp, code={}, message={}, requestId={}",
                resp.getCode(), resp.getMessage(), resp.getRequestId());
        if (!Objects.equals(resp.getCode(), 0L)) {
            log.warn("[OE_DISCOVERY] info batch failed, code={}, msg={}, requestId={}", resp.getCode(), resp.getMessage(), resp.getRequestId());
            return Collections.emptyList();
        }
        AgentAdvertiserInfoQueryV2ResponseData data = resp.getData();
        if (data == null || CollectionUtils.isEmpty(data.getAccountDetailList())) {
            log.warn("[OE_DISCOVERY] info batch empty, code={}, msg={}, requestId={}", resp.getCode(), resp.getMessage(), resp.getRequestId());
            return Collections.emptyList();
        }
        for (AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner item : data.getAccountDetailList()) {
            AdvertiserDetail dto = new AdvertiserDetail();
            dto.setChannelCode(channelCode);
            dto.setChannel(channel);
            dto.setAgentId(item.getFirstAgentId());
            dto.setAgentName(item.getFirstAgentName());
            dto.setAgentCompanyId(item.getFirstAgentCompanyId());
            dto.setAgentCompanyName(item.getFirstAgentCompanyName());
            dto.setAdvertiserId(item.getAdvertiserId());
            dto.setAdvertiserName(item.getAdvertiserName());
            dto.setAdvCompanyId(item.getAdvCompanyId());
            dto.setAdvCompanyName(item.getAdvCompanyName());
            dto.setFirstIndustryName(item.getFirstIndustryName());
            dto.setSecondIndustryName(item.getSecondIndustryName());
            dto.setSaleId(item.getSaleId());
            dto.setSaleName(item.getSaleName());
            dto.setAuthExpireDate(String.valueOf(item.getAuthExpireDate()));
            dto.setBindTime(String.valueOf(item.getBindTime()));
            dto.setCreateTimeOe(String.valueOf(item.getCreateTime()));
            result.add(dto);
        }
        return result;
    }

    private List<List<Long>> partition(List<Long> list, int size) {
        List<List<Long>> res = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            return res;
        }
        for (int i = 0; i < list.size(); i += size) {
            res.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return res;
    }

    @Data
    public static class AdvertiserDetail {
        private String channelCode;
        private String channel;
        private Long agentId;
        private String agentName;
        private Long agentCompanyId;
        private String agentCompanyName;
        private Long advertiserId;
        private String advertiserName;
        private String advertiserStatus;
        private Long advCompanyId;
        private String advCompanyName;
        private String firstIndustryName;
        private String secondIndustryName;
        private String accountSource;
        private Long saleId;
        private String saleName;
        private String authExpireDate;
        private String bindTime;
        private String createTimeOe;
    }
}
