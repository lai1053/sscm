package com.kakarote.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "oceanengine")
public class OceanEngineProperties {

    /**
     * 业务接口域名前缀
     */
    private String baseUrl = "https://api.oceanengine.com/open_api";

    /**
     * ADS 基础域名（不含路径）
     */
    private String adsBaseUrl = "https://api.oceanengine.com";

    /**
     * 千川基础域名（不含路径）
     */
    private String qcBaseUrl = "https://ad.oceanengine.com";

    /**
     * 暂时手填的 access_token
     */
    private String accessToken;

    /**
     * 主代理商ID
     */
    private Long primaryAgentId;

    /**
     * 每页大小
     */
    private Integer pageSize = 100;
}
