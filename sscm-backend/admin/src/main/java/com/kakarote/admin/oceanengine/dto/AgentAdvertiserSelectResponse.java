package com.kakarote.admin.oceanengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentAdvertiserSelectResponse {

    private Integer code;
    private String message;
    private String requestId;
    private DataWrapper data;

    @Data
    public static class DataWrapper {
        private List<Long> advertiserIds;
        private List<Long> list; // 和 advertiser_ids 一样
        private PageInfo pageInfo;
        private String accountSource;
    }

    @Data
    public static class PageInfo {
        private Integer page;
        private Integer pageSize;
        private Integer totalNumber;
        private Integer totalPage;
    }
}