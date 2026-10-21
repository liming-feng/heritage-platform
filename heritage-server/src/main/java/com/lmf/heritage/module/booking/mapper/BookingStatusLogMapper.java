package com.lmf.heritage.module.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.booking.entity.BookingStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约状态流转轨迹 Mapper
 *
 * <p>只用到 BaseMapper 的插入与按条件查询，不需要自定义 SQL。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface BookingStatusLogMapper extends BaseMapper<BookingStatusLog> {
}
