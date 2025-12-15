package com.kakarote.admin.oceanengine.model;

import lombok.Data;

/**
 * 登录用户的 OceanEngine 销售信息。
 */
@Data
public class OeLoginSaleInfoVO {
    private Long saleUserId;
    private Long saleId;
    private String saleName;
    private Long crmUserId;
    private String crmRealname;
}
