package com.lmf.heritage.module.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.log.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 *
 * <p>过滤条件（操作人 / 操作类型 / 角色 / 时间范围 / 成功与否）全部是单表等值与范围条件，
 * 用 MyBatis-Plus 的 {@code LambdaQueryWrapper} 就能表达清楚，因此这里不写 XML。</p>
 *
 * <p>反过来说，如果这里出现了「查出来还要去别的表补字段」的需求，
 * 就该老老实实写 XML —— 日志表刻意不存外键、也不 JOIN 用户表，
 * 就是为了让它永远是一次索引扫描，不会随着业务表变复杂而变慢。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
