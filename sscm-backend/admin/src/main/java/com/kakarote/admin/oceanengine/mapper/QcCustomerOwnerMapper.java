package com.kakarote.admin.oceanengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.admin.oceanengine.entity.QcCustomerOwner;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface QcCustomerOwnerMapper extends BaseMapper<QcCustomerOwner> {
}
