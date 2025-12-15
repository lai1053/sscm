package com.kakarote.admin.oceanengine.mapper;

import com.kakarote.admin.oceanengine.entity.QcOeSaleUser;
import com.kakarote.admin.oceanengine.model.SalesDashboardSummaryVO;
import com.kakarote.core.entity.BasePage;
import com.kakarote.core.servlet.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 巨量销售用户表 Mapper
 */
public interface QcOeSaleUserMapper extends BaseMapper<QcOeSaleUser> {

    java.util.List<SalesDashboardSummaryVO> listSalesSummary(BasePage<SalesDashboardSummaryVO> page, @Param("keyword") String keyword);
}
