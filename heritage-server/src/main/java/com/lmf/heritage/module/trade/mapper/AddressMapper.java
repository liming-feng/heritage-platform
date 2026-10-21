package com.lmf.heritage.module.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.trade.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址 Mapper（单表操作，BaseMapper 足够）
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
