package com.lmf.heritage.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.coupon.dto.AdminCouponQueryDTO;
import com.lmf.heritage.module.coupon.entity.Coupon;
import com.lmf.heritage.module.coupon.vo.AdminCouponVO;
import com.lmf.heritage.module.coupon.vo.CouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 优惠券 Mapper
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 管理端券种分页
     *
     * <p>带出两个与用户端不同的口径：</p>
     * <ul>
     *   <li>{@code usedCount} —— 已使用张数，核销率的分子；</li>
     *   <li>{@code receiveRecordCount} —— user_coupon 表实际记录数，用来与
     *       coupon.received_count 对账。两个数字本该一致，不一致说明有领取路径漏了自增，
     *       把它放在后台能一眼看出来。</li>
     * </ul>
     */
    IPage<AdminCouponVO> selectAdminCouponPage(IPage<AdminCouponVO> page, @Param("q") AdminCouponQueryDTO q);

    /** 管理端券种详情（同一套字段，按 id 查一条） */
    AdminCouponVO selectAdminCouponById(@Param("id") Long id);

    /** 券种维度总览：券种数、启用数、发放总量、已领取量 */
    Map<String, Object> statOverall();

    /** 按券类型分组的领取 / 使用情况 */
    List<Map<String, Object>> statByType();

    /**
     * 领券中心列表
     *
     * <p>只取「启用中 + 在有效期内」的券；<b>已领完的券也会返回</b>，
     * 由前端置灰显示「已抢光」——直接不返回会让用户以为这张券不存在。</p>
     */
    IPage<CouponVO> selectCenterPage(IPage<CouponVO> page, @Param("now") LocalDateTime now);

    /**
     * <b>领券的核心：带条件的原子自增，防超发</b>
     *
     * <pre>
     * UPDATE coupon
     * SET received_count = received_count + 1
     * WHERE id = #{id} AND status = 1 AND received_count &lt; total_count
     * </pre>
     *
     * <p>「判断还有没有剩余」与「扣减」发生在同一条 SQL 里并持有行锁，
     * 因此 100 个人同时抢最后 1 张券时，只有一个人能拿到（返回 1 行），
     * 其余人返回 0 行 → 抛「优惠券已被领完」。这与商品库存、
     * 预约名额用的是同一套思路：<b>正确性交给数据库的条件更新，不靠先查后改。</b></p>
     */
    @Update("UPDATE coupon SET received_count = received_count + 1, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND received_count < total_count")
    int increaseReceived(@Param("id") Long id);

    /**
     * 当前用户在各券种上已领取的张数（用于「每人限领」校验与领券中心按钮状态）
     *
     * <p>一次查出全部券种的已领数再在内存里映射，避免在列表循环里逐条查
     * ——那是典型的 N+1 查询。</p>
     */
    @Select("SELECT coupon_id AS couponId, COUNT(*) AS cnt FROM user_coupon "
            + "WHERE deleted = 0 AND user_id = #{userId} GROUP BY coupon_id")
    List<Map<String, Object>> countReceivedByCoupon(@Param("userId") Long userId);
}
