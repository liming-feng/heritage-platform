package com.lmf.heritage.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.coupon.dto.AdminUserCouponQueryDTO;
import com.lmf.heritage.module.coupon.entity.UserCoupon;
import com.lmf.heritage.module.coupon.vo.AdminUserCouponVO;
import com.lmf.heritage.module.coupon.vo.UserCouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户优惠券 Mapper
 *
 * <p>「领取」是插入，「使用」与「归还」都是<b>条件更新</b>——
 * 用 {@code WHERE status = 期望值} 保证同一张券不会被用两次、也不会被重复归还。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /** 管理端领取记录分页（联表带出券种与领取人） */
    IPage<AdminUserCouponVO> selectAdminReceivePage(IPage<AdminUserCouponVO> page,
                                                   @Param("q") AdminUserCouponQueryDTO q);

    /** 全平台领取记录总览：领取量、未使用、已使用、已过期 */
    Map<String, Object> statOverall();

    /** 某人某券已领张数（每人限领校验） */
    @Select("SELECT COUNT(*) FROM user_coupon WHERE deleted = 0 "
            + "AND user_id = #{userId} AND coupon_id = #{couponId}")
    int countByUserAndCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    /**
     * 我的优惠券分页（联表带出券种定义）
     *
     * @param status 0未使用 1已使用 2已过期，null 表示全部
     */
    IPage<UserCouponVO> selectMinePage(IPage<UserCouponVO> page,
                                       @Param("userId") Long userId,
                                       @Param("status") Integer status);

    /** 我的优惠券各状态数量（tab 角标） */
    @Select("SELECT status, COUNT(*) AS cnt FROM user_coupon "
            + "WHERE deleted = 0 AND user_id = #{userId} GROUP BY status")
    List<Map<String, Object>> countGroupByStatus(@Param("userId") Long userId);

    /**
     * 当前用户「未使用且未过期」的券（联表带出券种定义）
     *
     * <p>结算页的可用券就是这份数据过滤出来的：门槛判断与抵扣计算全部交给策略模式，
     * SQL 只负责把「还用得了」的券捞出来。</p>
     */
    List<UserCouponVO> selectUsable(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** 按 id 查一张券的完整视图（下单校验用，含券种的类型/门槛/折扣） */
    UserCouponVO selectVoById(@Param("id") Long id);

    /**
     * <b>使用优惠券：0 → 1 的条件更新</b>
     *
     * <p>返回 0 行说明这张券已经被用过（或不属于该用户），
     * 调用方据此拒绝下单，杜绝「同一张券抵扣两笔订单」。</p>
     */
    @Update("UPDATE user_coupon SET status = 1, order_no = #{orderNo}, "
            + "used_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND user_id = #{userId} AND status = 0 AND deleted = 0")
    int useCoupon(@Param("id") Long id,
                  @Param("userId") Long userId,
                  @Param("orderNo") String orderNo);

    /**
     * <b>归还优惠券：1 → 0</b>
     *
     * <p>订单取消 / 退款成功时把券还回去，让用户能重新使用。</p>
     *
     * <p>为什么按 {@code order_no} 而不是按券 id 归还：调用方（订单取消 / 退款审核）
     * 手上只有订单，让它去查「这单用了哪张券」再回传是多一次查询；
     * 而 order_no 本身就写在 user_coupon 上。<br>
     * 同时带 {@code status = 1} 前置条件，保证重复归还只生效一次。</p>
     */
    @Update("UPDATE user_coupon SET status = 0, order_no = NULL, used_time = NULL, update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND status = 1 AND deleted = 0")
    int returnByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 把已过期的未使用券标记为「已过期」（定时任务）
     *
     * <p>带 {@code status = 0} 条件，因此已经用掉的券不会被误标成过期。</p>
     */
    @Update("UPDATE user_coupon SET status = 2, update_time = NOW() "
            + "WHERE deleted = 0 AND status = 0 AND expire_time IS NOT NULL AND expire_time <= #{now}")
    int markExpired(@Param("now") LocalDateTime now);

    /**
     * 查「即将过期但还没提醒过」的券（过期前 {@code hours} 小时）
     *
     * <p>「没提醒过」不靠额外字段，而是查 message 表里有没有同一张用户券的优惠券消息
     * ——与开课提醒用同一套幂等思路，避免给 message 表加冗余标记位。</p>
     */
    @Select("SELECT * FROM user_coupon WHERE deleted = 0 AND status = 0 "
            + "AND expire_time IS NOT NULL AND expire_time > #{now} AND expire_time <= #{deadline}")
    List<UserCoupon> selectExpiringSoon(@Param("now") LocalDateTime now,
                                        @Param("deadline") LocalDateTime deadline);
}
