package com.lmf.heritage.module.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.trade.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper
 *
 * <p>列表与详情都是「先分页查主表、再批量查 order_item 组装」，避免 N+1，
 * 因此这里只需要 BaseMapper + 几个统计语句。</p>
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 用户端「我的订单」各状态数量（含"全部"，用于 tab 角标）
     *
     * @return [{status: 0, cnt: 2}, ...]
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM orders "
            + "WHERE deleted = 0 AND user_id = #{userId} GROUP BY status")
    List<Map<String, Object>> countGroupByStatusForUser(@Param("userId") Long userId);

    /** 匠人端各状态订单数量（P7 看板用） */
    @Select("SELECT status, COUNT(*) AS cnt FROM orders "
            + "WHERE deleted = 0 AND craftsman_id = #{craftsmanId} GROUP BY status")
    List<Map<String, Object>> countGroupByStatusForCraftsman(@Param("craftsmanId") Long craftsmanId);

    /** 判断某用户是否购买过该商品且订单已完成（评价资格校验用） */
    @Select("SELECT COUNT(*) FROM orders o JOIN order_item i ON i.order_id = o.id "
            + "WHERE o.deleted = 0 AND o.user_id = #{userId} AND i.goods_id = #{goodsId} AND o.status = 3")
    long countFinishedPurchase(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
}
