package com.lmf.heritage.module.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.trade.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单项 Mapper
 *
 * <p>order_item 表没有 deleted 字段（快照表，随订单逻辑删除即可），
 * 所以不要给它加 {@code @TableLogic}。</p>
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 按订单 id 集合批量取订单项（订单列表一次查完，避免 N+1）
     *
     * <p>用 {@code <script>} + {@code <foreach>} 让参数走预编译占位符，
     * 不要用 {@code ${}} 拼接（那是 SQL 注入点）。</p>
     */
    @Select("<script>"
            + "SELECT * FROM order_item WHERE order_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY id"
            + "</script>")
    List<OrderItem> listByOrderIds(@Param("ids") List<Long> orderIds);

    /** 把订单项标记为已评价 */
    @org.apache.ibatis.annotations.Update("UPDATE order_item SET reviewed = 1 WHERE id = #{id} AND reviewed = 0")
    int markReviewed(@Param("id") Long id);
}
