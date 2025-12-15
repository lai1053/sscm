package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * @author binlonglai
 */
@TableName("wk_qc_app_auth")
public class WkQcAppAuth {

    @TableId
    private Long id;

    /** 关联集成应用ID */
    private Long integrationAppId;

    /** 渠道ID，对应 wk_qc_channel.id */
    private Long channelId;

    /** 授权主体名称（比如 “广告管理员（手工录入）”） */
    private String externalOwnerName;

    private String remark;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String authCode;

    // ===== getter / setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIntegrationAppId() {
        return integrationAppId;
    }

    public void setIntegrationAppId(Long integrationAppId) {
        this.integrationAppId = integrationAppId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getExternalOwnerName() {
        return externalOwnerName;
    }

    public void setExternalOwnerName(String externalOwnerName) {
        this.externalOwnerName = externalOwnerName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }
}