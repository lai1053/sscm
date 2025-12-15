package com.kakarote.admin.oceanengine.enums;

/**
 * 客户健康状态
 */
public enum CustomerHealthStatus {
    ACTIVE,        // 近7天有消耗
    INACTIVE_30D,  // 近30天无消耗
    INACTIVE_90D   // 近90天无消耗
}
