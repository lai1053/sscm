package com.kakarote.admin.oceanengine.service;

import com.kakarote.admin.oceanengine.entity.QcOeAdvertiser;

/**
 * 将广告主与销售档案绑定（填充 sale_user_id）
 */
public interface OeAdvertiserSaleBindService {

    /**
     * 为现有广告主根据 sale_id 回填 sale_user_id，仅处理 sale_user_id 为空且 sale_id>0 的记录
     */
    void backfillSaleUserId();

    /**
     * 为单个广告主对象根据 sale_id 绑定 sale_user_id（不覆盖已有绑定）
     */
    void bindSaleUserForAdvertiser(QcOeAdvertiser advertiser);
}
