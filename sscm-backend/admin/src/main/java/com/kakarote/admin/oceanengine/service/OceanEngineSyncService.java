package com.kakarote.admin.oceanengine.service;

/**
 * @author dong
 * @date 2025-11-25 17:39
 */
public interface OceanEngineSyncService {
    /**
     * 全量同步：代理商 -> 广告主 -> 销售 & 广告主表
     */
    void syncAll();
}
