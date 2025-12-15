package com.kakarote.admin.oceanengine.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WuKong 与巨量销售账号绑定关系。
 */
@Data
@TableName("wk_qc_oe_user_mapping")
public class OeUserMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("admin_user_id")
    private Long adminUserId;

    @TableField("oe_sale_user_id")
    private Long oeSaleUserId;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
