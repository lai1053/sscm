package com.kakarote.admin.oceanengine.service;

/**
 * OceanEngine 身份绑定服务。
 */
public interface OeIdentityService {

    /**
     * 查询当前登录用户绑定的巨量销售用户 ID。
     *
     * @return 绑定 ID，未绑定返回 null
     */
    Long getCurrentOeSaleUserId();

    /**
     * 查询指定悟空用户绑定的巨量销售用户 ID。
     *
     * @param adminUserId 悟空用户ID
     * @return 绑定 ID，未绑定返回 null
     */
    Long getOeSaleUserIdByAdminUserId(Long adminUserId);

    /**
     * 绑定（或更新）WuKong 用户与巨量销售用户的关系。
     *
     * @param adminUserId  WuKong 用户 ID
     * @param oeSaleUserId 巨量销售用户 ID
     */
    void bindUser(Long adminUserId, Long oeSaleUserId);
}
