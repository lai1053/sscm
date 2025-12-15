package com.kakarote.admin.oceanengine.config;

/**
 * OceanEngine（ADS/千川）通用常量集中管理。
 * 规范：通用硬编码（渠道枚举值、接口路径、固定字段名等）必须放在此类，禁止分散在业务实现中。
 */
public final class OceanEngineConstants {

    private OceanEngineConstants() {
    }

    /**
     * 渠道枚举值：巨量广告（ADS），对应数据库字段 adv_channel / channel。
     */
    public static final String CHANNEL_ADS = "ADS";

    /**
     * 渠道枚举值：巨量千川（QIANCHUAN），对应数据库字段 adv_channel / channel。
     */
    public static final String CHANNEL_QIANCHUAN = "QIANCHUAN";

    /**
     * 巨量 ADS v3 自定义报表接口 path（BASIC_DATA）。
     * 文档：/open_api/v3.0/report/custom/get/
     */
    public static final String ADS_CUSTOM_REPORT_V3_PATH = "/open_api/v3.0/report/custom/get/";

    /**
     * 千川账号级报表接口 path。
     * 文档：/open_api/v1.0/qianchuan/report/advertiser/get/
     */
    public static final String QC_ADVERTISER_REPORT_PATH = "/open_api/v1.0/qianchuan/report/advertiser/get/";
}
