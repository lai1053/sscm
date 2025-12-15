package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理-广告主关系表：记录代理（根账号）以指定 account_source 发现的广告主关系。
 */
@Data
@TableName("wk_qc_oe_agent_advertiser_rel")
@ApiModel(value = "QcOeAgentAdvertiserRel对象", description = "代理-广告主关系")
public class QcOeAgentAdvertiserRel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("代理商根账号 advertiser_id")
    private Long rootAdvertiserId;

    @ApiModelProperty("代理账号类型 account_type")
    private String rootAccountType;

    @ApiModelProperty("代理账号角色 account_role")
    private String rootAccountRole;

    @ApiModelProperty("被管理的广告主 ID")
    private Long advertiserId;

    @ApiModelProperty("账号来源 account_source（AD/LOCAL/STAR/LUBAN/DOMESTIC 等）")
    private String accountSource;

    @ApiModelProperty("渠道，默认 ADS")
    private String channel;

    @ApiModelProperty("是否仍然存在于最近一次 select 结果：1-是，0-否")
    private Integer isActive;

    @ApiModelProperty("第一次在 select 结果中出现的时间")
    private Date firstSeen;

    @ApiModelProperty("最近一次在 select 结果中出现的时间")
    private Date lastSeen;

    @ApiModelProperty("最近一次处理时间")
    private Date lastSyncTime;

    private Date gmtCreate;

    private Date gmtModified;

    private Integer isDeleted;
}
