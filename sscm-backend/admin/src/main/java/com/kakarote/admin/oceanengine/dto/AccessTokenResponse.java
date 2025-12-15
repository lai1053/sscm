package com.kakarote.admin.oceanengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class AccessTokenResponse {

    private Integer code;
    private String message;
    private String requestId;
    private AccessTokenData data;

    @Data
    public static class AccessTokenData {
        private String accessToken;
        private List<Long> advertiserIds;
        private Long expiresIn;
        private String refreshToken;
        private Long refreshTokenExpiresIn;
    }
}