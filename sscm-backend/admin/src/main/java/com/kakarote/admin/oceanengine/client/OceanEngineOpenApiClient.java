package com.kakarote.admin.oceanengine.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.admin.oceanengine.enums.OceanChannelCode;
import com.kakarote.admin.oceanengine.service.OceanTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Objects;

@Component
public class OceanEngineOpenApiClient {

    private static final Logger log = LoggerFactory.getLogger(OceanEngineOpenApiClient.class);

    private static final String ADS_BASE_URL = "https://api.oceanengine.com";
    private static final String QIANCHUAN_BASE_URL = "https://ad.oceanengine.com";

    @Resource
    private OceanTokenService tokenService;

    @Resource
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 /open_api/oauth2/advertiser/get
     * 用来验证当前 token 有效 & 看这个 token 代表的主体
     */
    public JsonNode getAdvertiserByToken(OceanChannelCode channelCode) {
        String accessToken = tokenService.getAccessToken(channelCode);
        String baseUrl = Objects.equals(channelCode, OceanChannelCode.OCEANENGINE_ADS)
                ? ADS_BASE_URL
                : QIANCHUAN_BASE_URL;
        String url = baseUrl + "/open_api/oauth2/advertiser/get/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Access-Token", accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String respBody;
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            respBody = responseEntity.getBody();
        } catch (HttpClientErrorException e) {
            // 打印一下方便你排查
            log.error("调用 oauth2/advertiser/get 失败, channel={}, status={}, body={}",
                    channelCode.getCode(), e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }

        log.info("oauth2/advertiser/get resp, channel={}, body={}", channelCode.getCode(), respBody);

        try {
            return objectMapper.readTree(respBody);
        } catch (Exception e) {
            throw new RuntimeException("解析 oauth2/advertiser/get 返回失败: " + e.getMessage(), e);
        }
    }
}
