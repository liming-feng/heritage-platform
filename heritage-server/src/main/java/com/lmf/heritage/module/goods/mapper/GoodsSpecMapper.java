package com.lmf.heritage.module.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.goods.entity.GoodsSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品规格 Mapper
 *
 * <p>规格库存与商品总库存**同时扣减**：选规格的商品，规格库存决定能不能买，
 * 商品总库存作为总量上限一起扣，保证「总库存 = 各规格库存之和」不被破坏。</p>
 */
@Mapper
public interface GoodsSpecMapper extends BaseMapper<GoodsSpec> {

    /** 扣减规格库存，返回 0 表示该规格库存不足 */
    @Update("UPDATE goods_spec SET stock = stock - #{quantity}, update_time = NOW() "
            + "WHERE id = #{specId} AND deleted = 0 AND stock >= #{quantity}")
    int deductStock(@Param("specId") Long specId, @Param("quantity") int quantity);

    /** 归还规格库存 */
    @Update("UPDATE goods_spec SET stock = stock + #{quantity}, update_time = NOW() "
            + "WHERE id = #{specId} AND deleted = 0")
    int restoreStock(@Param("specId") Long specId, @Param("quantity") int quantity);
}
