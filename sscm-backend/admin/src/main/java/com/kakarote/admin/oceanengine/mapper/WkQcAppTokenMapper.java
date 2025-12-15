package com.kakarote.admin.oceanengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.admin.oceanengine.entity.WkQcAppToken;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @author binlonglai
 */
@Mapper
@Repository
public interface WkQcAppTokenMapper extends BaseMapper<WkQcAppToken> {
}