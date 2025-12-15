package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.util.Date;

/**
 * 销售名下广告主视图
 */
@Data
public class SalesAdvertiserVO {
    private Long advertiserId;
    private String advertiserName;
    private String channel;
    private String firstAgentName;
    private Long advCompanyId;
    private String advCompanyName;
    private String firstIndustryName;
    private String secondIndustryName;
    private String advertiserStatus;
    private Date lastSyncTime;
}
