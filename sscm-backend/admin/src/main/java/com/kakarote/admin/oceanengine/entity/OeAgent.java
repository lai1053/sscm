package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wk_qc_oe_agent")
public class OeAgent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;
    private String agentName;
    private String role;
    private String accountStatus;
    private Long companyId;
    private String companyName;
    private Long customerId;
    private String customerName;
    private LocalDateTime oeCreateTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer isDeleted;
}