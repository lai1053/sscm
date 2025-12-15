package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.service.impl.OeSaleUserProvisionServiceImpl.ProvisionResult;
import org.springframework.lang.Nullable;

/**
 * 基于巨量销售信息自动创建并绑定悟空 CRM 用户
 */
public interface OeSaleUserProvisionService {

    /**
     * 从广告主数据聚合销售，并确保对应 CRM 账号与映射存在
     * @param channelCode 可选：OCEANENGINE_ADS / OCEANENGINE_QIANCHUAN，null 表示全量
     * @return 处理统计
     */
    ProvisionResult syncSaleUsersFromAdvertisers(@Nullable String channelCode);

    /**
     * 确保指定 saleId 已绑定 CRM 用户，返回 login_user_id
     */
    Long ensureCrmUserForSale(Long saleId);
}
