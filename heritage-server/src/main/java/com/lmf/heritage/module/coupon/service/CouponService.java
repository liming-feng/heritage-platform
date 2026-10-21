package com.lmf.heritage.module.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.coupon.dto.CouponQueryDTO;
import com.lmf.heritage.module.coupon.entity.UserCoupon;
import com.lmf.heritage.module.coupon.vo.CouponAvailableResultVO;
import com.lmf.heritage.module.coupon.vo.CouponUseResultVO;
import com.lmf.heritage.module.coupon.vo.CouponVO;
import com.lmf.heritage.module.coupon.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 优惠券服务
 *
 * <p>抵扣金额的算法<b>全部委托给策略模式</b>（见 coupon.strategy 包），
 * 本服务只负责业务规则：谁能领、领几张、能不能用、用完怎么记账。</p>
 *
 * @author 李明峰 23100025
 */
public interface CouponService extends IService<UserCoupon> {

    /** 领券中心（启用中且在有效期内的券，含已领完的，附当前用户可领状态） */
    PageResult<CouponVO> center(CouponQueryDTO query);

    /**
     * 领取优惠券
     *
     * <p>两侧校验都是「原子条件更新」而非「先查后改」：</p>
     * <ol>
     *   <li>发放总量：{@code UPDATE ... WHERE received_count < total_count}；</li>
     *   <li>每人限领：先抢 Redis 锁把同一用户对同一券的并发领取串行化，再计数校验。</li>
     * </ol>
     */
    void receive(Long couponId);

    /** 我的优惠券（按状态筛选，未使用在前、快过期的更前） */
    PageResult<UserCouponVO> mine(CouponQueryDTO query);

    /** 我的优惠券各状态数量：key 0未使用 1已使用 2已过期 */
    Map<Integer, Integer> myCount();

    /**
     * 结算页可用券查询
     *
     * @param amount 订单金额
     * @param scope  本次消费类型，见 {@code Constants.TARGET_*}（1商品 2课程）；
     *               用于过滤「仅商品 / 仅课程」的券
     */
    CouponAvailableResultVO available(BigDecimal amount, Integer scope);

    /**
     * <b>试算优惠券</b>：只校验并计算，<b>不核销、不改任何状态</b>。
     *
     * <p>结算预览页需要「选了这张券能减多少」，但用户此时还没提交订单，
     * 绝不能把券标记成已使用。校验规则与 {@link #useForOrder} 完全一致
     * （归属 / 状态 / 有效期 / 适用范围 / 门槛），两条路径共用同一段实现，
     * 避免「预览说能减 30、下单却报不可用」的不一致。</p>
     *
     * @return 抵扣金额，保留 2 位小数
     */
    BigDecimal previewDiscount(Long userCouponId, BigDecimal amount, Integer scope);

    /**
     * <b>下单时核销优惠券</b>：校验归属 / 状态 / 有效期 / 适用范围 / 门槛，
     * 用条件更新把券置为已使用并写入订单号，返回券种、抵扣金额等信息供订单落库。
     *
     * <p>注意：本方法<b>不自己开事务</b>，它必须运行在下单的同一个事务里——
     * 否则会出现「券已标记使用，订单却因库存不足回滚」的净损失。</p>
     */
    CouponUseResultVO useForOrder(Long userCouponId, BigDecimal amount, Integer scope, String orderNo);

    /** 订单取消 / 退款成功 → 把用掉的券还给用户（按订单号，幂等） */
    void giveBackByOrderNo(String orderNo);

    /* ==================== 定时任务入口 ==================== */

    /** 把过期的未使用券标记为已过期，返回处理条数 */
    int markExpired();

    /** 给「即将过期」的券发提醒消息（靠 message 表去重，可重复执行），返回发送条数 */
    int sendExpireReminders();
}
