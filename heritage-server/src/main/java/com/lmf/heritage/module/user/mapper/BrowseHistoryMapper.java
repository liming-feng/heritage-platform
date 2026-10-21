package com.lmf.heritage.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.user.entity.BrowseHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 浏览历史 Mapper
 */
@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {
}
