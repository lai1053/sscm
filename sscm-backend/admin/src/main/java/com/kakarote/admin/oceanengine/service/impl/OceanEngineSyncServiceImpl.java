package com.kakarote.admin.oceanengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytedance.ads.ApiClient;
import com.bytedance.ads.ApiException;
import com.bytedance.ads.api.AgentAdvertiserInfoQueryV2Api;
import com.bytedance.ads.api.AgentAdvertiserSelectV2Api;
import com.bytedance.ads.model.AgentAdvertiserInfoQueryV2Response;
import com.bytedance.ads.model.AgentAdvertiserInfoQueryV2ResponseData;
import com.bytedance.ads.model.AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner;
import com.bytedance.ads.model.AgentAdvertiserSelectV2Filtering;
import com.bytedance.ads.model.AgentAdvertiserSelectV2Response;
import com.bytedance.ads.model.AgentAdvertiserSelectV2ResponseData;
import com.bytedance.ads.model.AgentAdvertiserSelectV2ResponseDataCursorPageInfo;
import com.kakarote.admin.config.OceanEngineProperties;
import com.kakarote.admin.oceanengine.config.OceanEngineConstants;
import com.kakarote.admin.oceanengine.entity.OeAdvertiser;
import com.kakarote.admin.oceanengine.entity.OeSaleUser;
import com.kakarote.admin.oceanengine.mapper.OeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.OeSaleUserMapper;
import com.kakarote.admin.oceanengine.service.OceanEngineSyncService;
import com.kakarote.admin.oceanengine.service.OeAgentAdvertiserRelationService;
import com.kakarote.admin.oceanengine.util.PinyinHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同步巨量客户数据到 CRM
 * @author binlonglai
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class OceanEngineSyncServiceImpl implements OceanEngineSyncService {

    private final OceanEngineProperties properties;
    private final OeSaleUserMapper saleUserMapper;
    private final OeAdvertiserMapper advertiserMapper;
    private final OeAgentAdvertiserRelationService relationService;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private AgentAdvertiserSelectV2Api agentAdvertiserSelectV2Api;

    @Resource
    private AgentAdvertiserInfoQueryV2Api agentAdvertiserInfoQueryV2Api;

    @Resource
    private ApiClient oceanEngineApiClient;

    @Override
    public void syncAll() {
        Long agentId = properties.getPrimaryAgentId();
        String accessToken = properties.getAccessToken();
        Integer pageSize = properties.getPageSize() == null ? 100 : properties.getPageSize();

        if (agentId == null || accessToken == null) {
            log.error("[OE_SYNC] primaryAgentId 或 accessToken 未配置");
            return;
        }

        // 每次任务开始，把 Access-Token 写到 SDK 的默认 header 里
        oceanEngineApiClient.addDefaultHeader("Access-Token", accessToken);

        log.info("[OE_SYNC] start syncAll by SDK, agentId={}", agentId);

        // 统一使用 cursor 模式，支持超大规模广告主列表
        Long cursor = null;
        int loop = 0;
        List<Long> allAdvertisers = new ArrayList<>();
        String accountSource = null;

        try {
            while (true) {
                AgentAdvertiserSelectV2Response resp =
                        agentAdvertiserSelectV2Api.openApi2AgentAdvertiserSelectGet(
                                agentId,
                                null,               // companyIds
                                pageSize.longValue(), // count
                                cursor,             // cursor
                                (AgentAdvertiserSelectV2Filtering) null, // filtering
                                null,               // page
                                null                // pageSize
                        );

                if (resp == null || resp.getData() == null) {
                    log.warn("[OE_SYNC] advertiser select empty, cursor={}", cursor);
                    break;
                }

                AgentAdvertiserSelectV2ResponseData data = resp.getData();
                if (data.getList() == null || data.getList().isEmpty()) {
                    log.warn("[OE_SYNC] advertiser list empty, cursor={}", cursor);
                    break;
                }

                if (accountSource == null) {
                    accountSource = data.getAccountSource();
                }
                List<Long> advertiserIds = data.getList();
                allAdvertisers.addAll(advertiserIds);
                log.info("[OE_SYNC] cursorLoop={} cursor={} advertiser size={}", ++loop, cursor, advertiserIds.size());

                List<List<Long>> partitions = partition(advertiserIds, 50);
                for (List<Long> part : partitions) {
                    syncAdvertiserBatchBySdk(part);
                }

                AgentAdvertiserSelectV2ResponseDataCursorPageInfo cursorPageInfo = data.getCursorPageInfo();
                Boolean hasMore = cursorPageInfo == null ? null : cursorPageInfo.getHasMore();
                Long nextCursor = cursorPageInfo == null ? null : cursorPageInfo.getCursor();
                if (Boolean.FALSE.equals(hasMore) || nextCursor == null || nextCursor == 0L) {
                    break;
                }
                cursor = nextCursor;
            }

        } catch (ApiException e) {
            log.error("[OE_SYNC] syncAll ApiException", e);
        } catch (Exception e) {
            log.error("[OE_SYNC] syncAll unexpected error", e);
        }

        if (!allAdvertisers.isEmpty()) {
            relationService.syncRelations(agentId, "", "", accountSource, allAdvertisers, OceanEngineConstants.CHANNEL_ADS);
        }

        log.info("[OE_SYNC] syncAll finished");
    }

    /**
     * 使用 SDK 调用 /2/agent/advertiser_info/query/ 同步一批广告主
     */
    private void syncAdvertiserBatchBySdk(List<Long> advertiserIds) {
        if (advertiserIds == null || advertiserIds.isEmpty()) {
            return;
        }
        try {
            // 注意：这里的方法签名你本地是：
            // openApi2AgentAdvertiserInfoQueryGet(List<Long> accountIds, List<Long> companyIds, Long page, Long pageSize, ...?)
            // 我根据接口文档大致写成 accountIds + 其它参数为 null，
            // 你在 IDEA 里按 ⌘+P / Ctrl+P 看一下实际参数顺序，对应改一下即可。
            AgentAdvertiserInfoQueryV2Response resp =
                    agentAdvertiserInfoQueryV2Api.openApi2AgentAdvertiserInfoQueryGet(
                            advertiserIds
                    );

            if (resp == null || resp.getData() == null) {
                log.warn("[OE_SYNC] advertiser_info empty for ids={}", advertiserIds);
                return;
            }

            AgentAdvertiserInfoQueryV2ResponseData data = resp.getData();
            List<AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner> details =
                    data.getAccountDetailList();

            if (details == null || details.isEmpty()) {
                log.warn("[OE_SYNC] advertiser_info accountDetailList empty for ids={}", advertiserIds);
                return;
            }

            Map<Long, Long> saleIdToSaleUserId = upsertSales(details);
            upsertAdvertisers(details, saleIdToSaleUserId);

        } catch (ApiException e) {
            log.error("[OE_SYNC] syncAdvertiserBatch ApiException, ids={}", advertiserIds, e);
        } catch (Exception e) {
            log.error("[OE_SYNC] syncAdvertiserBatch unexpected error, ids={}", advertiserIds, e);
        }
    }

    /**
     * 先同步销售表（使用 SDK 的 AccountDetail 模型）
     */
    private Map<Long, Long> upsertSales(List<AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner> details) {
        Map<Long, AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner> saleMap = details.stream()
                .filter(d -> d.getSaleId() != null && d.getSaleId() > 0)
                .collect(Collectors.toMap(
                        AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner::getSaleId,
                        d -> d,
                        (a, b) -> a
                ));

        Map<Long, Long> result = new HashMap<>();

        for (Map.Entry<Long, AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner> entry : saleMap.entrySet()) {
            Long saleId = entry.getKey();
            String saleName = Optional.ofNullable(entry.getValue().getSaleName()).orElse("");

            OeSaleUser exist = saleUserMapper.selectOne(
                    new LambdaQueryWrapper<OeSaleUser>().eq(OeSaleUser::getSaleId, saleId));

            if (exist == null) {
                String loginUsername = genLoginUsername(saleName);
                OeSaleUser user = new OeSaleUser();
                user.setSaleId(saleId);
                user.setSaleName(saleName);
                user.setLoginUsername(loginUsername);
                user.setStatus(1);
                user.setSource("OCEANENGINE");
                // TODO: 在这里调用悟空原来的用户模块，创建一个系统用户，拿到 userId 填到 loginUserId
                saleUserMapper.insert(user);
                result.put(saleId, user.getId());
            } else {
                exist.setSaleName(saleName);
                saleUserMapper.updateById(exist);
                result.put(saleId, exist.getId());
            }
        }
        return result;
    }

    private void upsertAdvertisers(
            List<AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner> details,
            Map<Long, Long> saleIdToSaleUserId) {

        for (AgentAdvertiserInfoQueryV2ResponseDataAccountDetailListInner d : details) {
            Long advertiserId = d.getAdvertiserId();
            if (advertiserId == null) {
                continue;
            }

            OeAdvertiser exist = advertiserMapper.selectOne(
                    new LambdaQueryWrapper<OeAdvertiser>()
                            .eq(OeAdvertiser::getAdvertiserId, advertiserId));

            boolean isNew = false;
            if (exist == null) {
                exist = new OeAdvertiser();
                exist.setAdvertiserId(advertiserId);
                isNew = true;
            }

            // 下面这些字段都直接从 SDK 的 model 对应 getter 取：
            exist.setAdvertiserName(d.getAdvertiserName());
            exist.setAdvertiserStatus(String.valueOf(d.getAdvertiserStatus()));
            exist.setAdvCompanyId(d.getAdvCompanyId());
            exist.setAdvCompanyName(d.getAdvCompanyName());
            exist.setFirstAgentId(d.getFirstAgentId());
            exist.setFirstAgentName(d.getFirstAgentName());
            exist.setFirstAgentCompanyId(d.getFirstAgentCompanyId());
            exist.setFirstAgentCompanyName(d.getFirstAgentCompanyName());
            exist.setFirstIndustryName(d.getFirstIndustryName());
            exist.setSecondIndustryName(d.getSecondIndustryName());

            if (notEmpty(d.getAuthExpireDate())) {
                exist.setAuthExpireDate(LocalDate.parse(d.getAuthExpireDate(), D));
            }
            if (notEmpty(d.getBindTime())) {
                exist.setBindTime(LocalDateTime.parse(d.getBindTime(), DT));
            }
            if (notEmpty(d.getCreateTime())) {
                exist.setCreateTimeOe(LocalDateTime.parse(d.getCreateTime(), DT));
            }

            exist.setSaleId(d.getSaleId());
            exist.setSaleName(d.getSaleName());
            if (d.getSaleId() != null) {
                exist.setSaleUserId(saleIdToSaleUserId.get(d.getSaleId()));
            }

            exist.setContactName(d.getContactName());
            exist.setCustomerSaleName(d.getCustomerSaleName());
            exist.setOptimizerId(d.getOptimizerId());
            exist.setOptimizerName(Optional.ofNullable(d.getOptimizerName()).orElse(""));
            exist.setBrandOptimizerId(d.getBrandOptimizerId());
            exist.setBrandOptimizerName(Optional.ofNullable(d.getBrandOptimizerName()).orElse(""));

            exist.setSelfOperationTag(String.valueOf(d.getSelfOperationTag()));
            exist.setLastSyncTime(LocalDateTime.now());

            if (isNew) {
                advertiserMapper.insert(exist);
            } else {
                advertiserMapper.updateById(exist);
            }
        }
    }

    private String genLoginUsername(String saleName) {
        String base = PinyinHelper.toPinyin(saleName);
        if (base.isEmpty()) {
            base = "user";
        }
        String candidate = base;
        int idx = 1;
        while (true) {
            Long count = Long.valueOf(saleUserMapper.selectCount(
                    new LambdaQueryWrapper<OeSaleUser>()
                            .eq(OeSaleUser::getLoginUsername, candidate)
            ));
            if (count == 0) {
                return candidate;
            }
            candidate = base + idx;
            idx++;
        }
    }

    private boolean notEmpty(String s) {
        return s != null && !s.isEmpty() && !"0000-00-00".equals(s);
    }

    private List<List<Long>> partition(List<Long> list, int size) {
        List<List<Long>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
