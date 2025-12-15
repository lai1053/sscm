package com.kakarote.admin.oceanengine.service;

import java.util.List;

/**
 * 代理-广告主关系维护服务：持久化 agent/advertiser/select 结果。
 */
public interface OeAgentAdvertiserRelationService {

    /**
     * 同步某代理（根账号）在特定 account_source 下发现的广告主列表。
     *
     * @param rootAdvertiserId 代理根账号 advertiser_id
     * @param rootAccountType  代理 account_type（为空可传 ""）
     * @param rootAccountRole  代理 account_role（为空可传 ""）
     * @param accountSource    select 返回的 account_source
     * @param advertiserIds    本次 select 返回的广告主 ID 列表
     * @param channel          渠道（默认 ADS）
     */
    void syncRelations(Long rootAdvertiserId,
                       String rootAccountType,
                       String rootAccountRole,
                       String accountSource,
                       List<Long> advertiserIds,
                       String channel);
}
