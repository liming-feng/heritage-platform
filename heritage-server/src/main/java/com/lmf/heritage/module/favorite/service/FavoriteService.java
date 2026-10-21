package com.lmf.heritage.module.favorite.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.favorite.dto.FavoriteDTO;
import com.lmf.heritage.module.favorite.dto.FavoriteQueryDTO;
import com.lmf.heritage.module.favorite.entity.Favorite;
import com.lmf.heritage.module.favorite.vo.FavoriteVO;

import java.util.Map;

/**
 * 收藏服务（商品 / 课程 / 资讯共用）
 *
 * @author 李明峰 23100025
 */
public interface FavoriteService extends IService<Favorite> {

    /** 收藏（幂等：重复收藏不报错，也不产生第二条记录） */
    void add(FavoriteDTO dto);

    /** 取消收藏（幂等：本来就没收藏时静默返回） */
    void remove(FavoriteDTO dto);

    /** 是否已收藏；未登录返回 false */
    boolean isFavorited(Integer targetType, Long targetId, Long userId);

    /** 我的收藏分页 */
    PageResult<FavoriteVO> mine(FavoriteQueryDTO query);

    /** 我的收藏各类型数量：key 1商品 2课程 3资讯 */
    Map<Integer, Integer> myCount();
}
