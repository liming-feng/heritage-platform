package com.lmf.heritage.module.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.booking.entity.BookingConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约规则配置 Mapper
 *
 * <p>配置表结构简单，增删改查全部沿用 BaseMapper 的通用方法即可。</p>
 */
@Mapper
public interface BookingConfigMapper extends BaseMapper<BookingConfig> {
}
