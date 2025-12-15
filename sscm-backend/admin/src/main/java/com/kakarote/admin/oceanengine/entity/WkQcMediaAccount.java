package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("wk_qc_media_account")
public class WkQcMediaAccount {

    @TableId
    private Long id;

    /** 对应 wk_qc_channel.id */
    private Long channelId;

    /** 平台侧账户ID：advertiser_id / account_id 等 */
    private String externalAccountId;

    /** 账户类型：AGENT / PLATFORM_ROLE_LOCAL_AGENT / PLATFORM_ROLE_STAR_AGENT / PLATFORM_ROLE_QIANCHUAN_AGENT 等 */
    private String accountType;

    /** 账户名称 / 店铺名 */
    private String name;

    /** 公司主体名称（有则存） */
    private String companyName;

    /** 父级媒体账户ID（暂时用不上，先留） */
    private Long parentAccountId;

    /** 默认通过哪条授权(app_auth)去操作此账户 */
    private Long appAuthId;

    /** 1=正常，0=停用 */
    private Integer status;

    /** 平台特有字段：account_role / advertiser_role / is_valid 等 */
    private String extra;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ===== getter / setter =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public void setExternalAccountId(String externalAccountId) {
        this.externalAccountId = externalAccountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Long getParentAccountId() {
        return parentAccountId;
    }

    public void setParentAccountId(Long parentAccountId) {
        this.parentAccountId = parentAccountId;
    }

    public Long getAppAuthId() {
        return appAuthId;
    }

    public void setAppAuthId(Long appAuthId) {
        this.appAuthId = appAuthId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
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
}