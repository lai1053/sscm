package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 渠道表
 * 对应表：wk_qc_channel
 */
@Data
@TableName("wk_qc_channel")
public class WkQcChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 渠道编码，例如：OCEANENGINE_ADS / OCEANENGINE_QIANCHUAN */
    private String code;

    /** 渠道名称，例如：巨量广告、巨量千川 */
    private String name;

    /** 状态：1=启用，0=停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}