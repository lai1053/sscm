package com.kakarote.admin.oceanengine.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InactiveCompanyVO {
    private Long advCompanyId;
    private String advCompanyName;
    private Long saleUserId;
    private String saleUserName;
    private LocalDate lastConsumeDate;

    @Data
    public static class SaleInfo {
        private Long saleUserId;
        private String saleUserName;
    }
}
