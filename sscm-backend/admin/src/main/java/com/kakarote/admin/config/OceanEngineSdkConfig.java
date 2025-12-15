package com.kakarote.admin.config;

import com.bytedance.ads.ApiClient;
import com.bytedance.ads.api.AgentAdvertiserSelectV2Api;
import com.bytedance.ads.api.AgentAdvertiserInfoQueryV2Api;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用来管理巨量 SDK 的配置类
 */

@Configuration
public class OceanEngineSdkConfig {

    @Bean
    public ApiClient oceanEngineApiClient() {
        ApiClient apiClient = new ApiClient();
        // 这里用巨量开放平台推荐的域名
        apiClient.setBasePath("https://api.oceanengine.com");
        // Access-Token 不要在这里写死，每次 Job 前动态设置
        String token = "";
        apiClient.addDefaultHeader("Access-Token", token);
        return apiClient;
    }

    @Bean
    public AgentAdvertiserSelectV2Api agentAdvertiserSelectV2Api(ApiClient oceanEngineApiClient) {
        AgentAdvertiserSelectV2Api api = new AgentAdvertiserSelectV2Api();
        api.setApiClient(oceanEngineApiClient);
        return api;
    }

    @Bean
    public AgentAdvertiserInfoQueryV2Api agentAdvertiserInfoQueryV2Api(ApiClient oceanEngineApiClient) {
        AgentAdvertiserInfoQueryV2Api api = new AgentAdvertiserInfoQueryV2Api();
        api.setApiClient(oceanEngineApiClient);
        return api;
    }
}