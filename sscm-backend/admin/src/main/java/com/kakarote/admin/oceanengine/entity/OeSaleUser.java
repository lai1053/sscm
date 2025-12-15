package com.kakarote.admin.oceanengine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author dong
 * @date 2025-11-25 16:54
 */

@Data
@TableName("wk_qc_oe_sale_user")
public class OeSaleUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long saleId;
    private String saleName;
    private String loginUsername;
    private Long loginUserId;
    private Integer status;
    private String source;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer isDeleted;
}