package com.kakarote.admin.oceanengine.service;

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.kakarote.admin.oceanengine.entity.WkQcMediaAccount;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;

public interface MediaAccountSyncService {

    /**
     * 全量同步当前授权下的 媒体账户（巨量广告 + 千川）
     * 先做简单版：只同步顶层平台账号，不展开子广告主。
     */
    void syncCurrentAuthMediaAccounts() throws UnsupportedEncodingException;

    /**
     * 按渠道查询媒体账户（后端/前端展示用）
     * @param channelCode 渠道枚举
     * @param status      状态，可空，1=正常，0=停用
     */
    List<WkQcMediaAccount> listMediaAccounts(OceanChannelCode channelCode, Integer status);
}