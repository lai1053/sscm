package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.service.impl.OeAccountDiscoveryServiceImpl.AdvertiserDetail;

import java.util.List;

/**
 * 统一的巨量账户发现服务，只负责调用外部接口并返回标准化的广告主明细
 */
public interface OeAccountDiscoveryService {

    /**
     * 按渠道发现可见的广告主账户
     * @param channelCode OCEANENGINE_ADS / OCEANENGINE_QIANCHUAN
     * @return 广告主明细列表
     */
    List<AdvertiserDetail> discoverAdvertisers(String channelCode);
}
