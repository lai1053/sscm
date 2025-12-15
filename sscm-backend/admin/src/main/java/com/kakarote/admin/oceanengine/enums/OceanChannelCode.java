package com.kakarote.admin.oceanengine.enums;

/**
 * @author 渠道枚举
 *
 */
public enum OceanChannelCode {

    OCEANENGINE_ADS(1L, "OCEANENGINE_ADS", "巨量广告"),
    OCEANENGINE_QIANCHUAN(2L, "OCEANENGINE_QIANCHUAN", "巨量千川");

    private final Long channelId;
    private final String code;
    private final String desc;

    OceanChannelCode(Long channelId, String code, String desc) {
        this.channelId = channelId;
        this.code = code;
        this.desc = desc;
    }

    public Long getChannelId() {
        return channelId;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OceanChannelCode fromCode(String code) {
        for (OceanChannelCode value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("无效的渠道编码: " + code);
    }
}