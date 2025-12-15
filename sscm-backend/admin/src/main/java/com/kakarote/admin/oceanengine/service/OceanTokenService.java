package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.enums.OceanChannelCode;

public interface OceanTokenService {

    /**
     * 根据渠道获取当前可用的 access_token
     * （当前每个渠道只有一个 app_auth，所以可以这么用）
     */
    String getAccessToken(OceanChannelCode channelCode);

    /**
     * 按 app_auth_id 获取 token
     */
    String getAccessTokenByAppAuth(Long appAuthId);

    /**
     * 刷新指定 app_auth 的 token（手动或定时任务入口）
     */
    void refreshTokenByAppAuth(Long appAuthId);

    /**
     * 刷新某个渠道下所有 app_auth 的 token
     */
    void refreshAllTokensForChannel(OceanChannelCode channelCode);

}