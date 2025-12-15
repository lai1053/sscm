package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.service.impl.OeAccountDiscoveryServiceImpl.AdvertiserDetail;
import com.kakarote.admin.oceanengine.service.impl.OeAdvertiserSyncServiceImpl.OeAdvertiserSyncResult;

import java.util.List;

/**
 * 广告主落库同步服务
 */
public interface OeAdvertiserSyncService {

    /**
     * 将发现到的广告主/代理商信息落库
     * @param details 发现结果
     * @return 同步结果统计
     */
    OeAdvertiserSyncResult syncAdvertisers(List<AdvertiserDetail> details);
}
