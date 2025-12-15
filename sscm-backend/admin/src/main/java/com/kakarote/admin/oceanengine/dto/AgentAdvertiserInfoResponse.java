package com.kakarote.admin.oceanengine.dto;

import lombok.Data;

import java.util.List;
/**
 * @author dong
 * @date 2025-11-25 17:11
 * 领导负责作选择，你负责快速推进。不用担心写代码对接 API 的问题，我这里跑了8个你这样的小服务
 */
@Data
public class AgentAdvertiserInfoResponse {

    private Integer code;
    private String message;
    private String requestId;
    private DataWrapper data;

    @Data
    public static class DataWrapper {
        private List<AccountDetail> accountDetailList;
    }

    @Data
    public static class AccountDetail {
        private Long advCompanyId;
        private String advCompanyName;
        private Long advertiserId;
        private String advertiserName;
        private String advertiserStatus;
        private String authExpireDate;
        private String bindTime;
        private Long firstAgentCompanyId;
        private String firstAgentCompanyName;
        private Long firstAgentId;
        private String firstAgentName;
        private String firstIndustryName;
        private String secondIndustryName;
        private Long saleId;
        private String saleName;
        private String selfOperationTag;
        private String createTime;
    }

}
