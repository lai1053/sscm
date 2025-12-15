package com.kakarote.admin.oceanengine.service;

import java.util.List;

/**
 * 解析 OceanEngine 看板可见销售范围。
 */
public interface OeDashboardScopeService {

    /**
     * 解析当前用户在销售看板查询时可使用的销售账号列表。
     *
     * @param requestSaleUserId 前端传入的 saleUserId（仅管理员生效）
     * @return null 表示不加 sale_user_id 过滤；空列表表示无可见销售
     */
    List<Long> resolveEffectiveSaleUserIds(Long requestSaleUserId);
}
