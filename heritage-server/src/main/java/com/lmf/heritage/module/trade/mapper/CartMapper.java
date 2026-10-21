package com.lmf.heritage.module.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.trade.entity.Cart;
import com.lmf.heritage.module.trade.vo.CartItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 购物车 Mapper
 *
 * <p>列表要展示商品的**实时**名称、封面、价格与库存（购物车不存快照，
 * 快照是下单时才做的事），所以走 XML 里的联表查询。</p>
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 查询用户购物车（联表出商品与规格的实时信息）
     *
     * @param userId 用户 id
     * @return 购物车项列表，含商品已下架 / 库存不足等失效标记
     */
    List<CartItemVO> selectCartList(@Param("userId") Long userId);

    /** 按 id 集合查询属于该用户的购物车项（结算时校验归属，防越权） */
    List<CartItemVO> selectByIdsAndUser(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /** 批量更新勾选状态 */
    int updateChecked(@Param("userId") Long userId,
                      @Param("ids") List<Long> ids,
                      @Param("checked") Integer checked);

    /** 清空已勾选项（下单成功后调用） */
    int deleteChecked(@Param("userId") Long userId);

    /**
     * <b>查同款购物车项（含已被逻辑删除的记录）</b>
     *
     * <p>为什么必须包含已删除的：cart 表上有唯一键 {@code uk_user_goods_spec}，
     * 而删除走的是逻辑删除（deleted=1）。如果加购前只查未删除的记录，
     * 用户「加入购物车 → 删除 → 再次加入」时就会撞唯一键报错。
     * 这里把已删除的行也查出来，由业务层决定是「复活」它还是累加数量。</p>
     *
     * <p>{@code spec_id} 可为 null，而 SQL 里 {@code = NULL} 永远不成立，
     * 必须用 {@code IS NULL} 单独处理。</p>
     */
    @Select("<script>"
            + "SELECT * FROM cart WHERE user_id = #{userId} AND goods_id = #{goodsId} "
            + "<choose>"
            + "  <when test='specId != null'>AND spec_id = #{specId}</when>"
            + "  <otherwise>AND spec_id IS NULL</otherwise>"
            + "</choose> "
            + "LIMIT 1"
            + "</script>")
    Cart selectIncludingDeleted(@Param("userId") Long userId,
                                @Param("goodsId") Long goodsId,
                                @Param("specId") Long specId);

    /**
     * 复活一条被逻辑删除的购物车项（把 deleted 置回 0 并重设数量与勾选）
     *
     * <p>用自定义 SQL 而不是 {@code updateById}：逻辑删除字段被 MyBatis-Plus
     * 排除在普通 update 之外，改不动它。</p>
     */
    @Update("UPDATE cart SET deleted = 0, quantity = #{quantity}, checked = 1, update_time = NOW() "
            + "WHERE id = #{id}")
    int revive(@Param("id") Long id, @Param("quantity") Integer quantity);
}
