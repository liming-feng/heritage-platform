package com.lmf.heritage.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.coupon.dto.CouponQueryDTO;
import com.lmf.heritage.module.coupon.entity.Coupon;
import com.lmf.heritage.module.coupon.entity.UserCoupon;
import com.lmf.heritage.module.coupon.mapper.CouponMapper;
import com.lmf.heritage.module.coupon.mapper.UserCouponMapper;
import com.lmf.heritage.module.coupon.service.CouponService;
import com.lmf.heritage.module.coupon.strategy.CouponStrategyFactory;
import com.lmf.heritage.module.coupon.vo.CouponAvailableResultVO;
import com.lmf.heritage.module.coupon.vo.CouponAvailableVO;
import com.lmf.heritage.module.coupon.vo.CouponUseResultVO;
import com.lmf.heritage.module.coupon.vo.CouponVO;
import com.lmf.heritage.module.coupon.vo.UserCouponVO;
import com.lmf.heritage.module.message.service.MessageService;
import com.lmf.heritage.security.UserContext;
import com.lmf.heritage.util.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 优惠券服务实现
 *
 * <p><b>三处容易写错的地方，这里都按「不做重复劳动、不留竞态」处理：</b></p>
 * <ol>
 *   <li><b>抵扣怎么算</b>：一律走 {@link CouponStrategyFactory}，本类里没有任何
 *       {@code if (type == 1)} 形式的金额计算；</li>
 *   <li><b>发券防超发</b>：总量靠 {@code UPDATE ... WHERE received_count < total_count} 原子扣减，
 *       每人限领靠 Redis 锁把同一用户的并发领取串行化；</li>
 *   <li><b>用券防重复</b>：{@code UPDATE ... WHERE status = 0} 条件更新，
 *       返回 0 行说明券已被用过，直接拒绝下单。</li>
 * </ol>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements CouponService {

    private static final Map<Integer, String> TYPE_DESC = Map.of(
            Constants.COUPON_TYPE_FULL_REDUCTION, "满减",
            Constants.COUPON_TYPE_DISCOUNT, "折扣");

    private static final Map<Integer, String> SCOPE_DESC = Map.of(
            Constants.COUPON_SCOPE_ALL, "全场通用",
            Constants.COUPON_SCOPE_GOODS, "仅商品",
            Constants.COUPON_SCOPE_COURSE, "仅课程");

    private static final Map<Integer, String> STATUS_DESC = Map.of(
            Constants.USER_COUPON_UNUSED, "未使用",
            Constants.USER_COUPON_USED, "已使用",
            Constants.USER_COUPON_EXPIRED, "已过期");

    /** 领券抢锁的等待 / 持有时间（毫秒） */
    private static final long RECEIVE_LOCK_WAIT_MILLIS = 3_000L;
    private static final long RECEIVE_LOCK_LEASE_MILLIS = 10_000L;

    /** 过期前多少小时发提醒 */
    private static final int EXPIRE_REMIND_HOURS = 24;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponStrategyFactory strategyFactory;
    private final RedisLockUtil redisLockUtil;
    private final TransactionTemplate txTemplate;
    private final MessageService messageService;

    /* ==================================================================
     *  领券中心
     * ================================================================== */

    @Override
    public PageResult<CouponVO> center(CouponQueryDTO query) {
        query.normalize();
        Long userId = UserContext.userId();

        Page<CouponVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<CouponVO> result = couponMapper.selectCenterPage(page, LocalDateTime.now());

        // 一次查出「我在各券种上领了几张」，避免在下面的循环里逐条查库（N+1）
        Map<Long, Integer> receivedMap = receivedCountMap(userId);

        List<CouponVO> list = new ArrayList<>(result.getRecords().size());
        for (CouponVO vo : result.getRecords()) {
            vo.setTypeDesc(TYPE_DESC.getOrDefault(vo.getType(), "优惠券"));
            vo.setScopeDesc(SCOPE_DESC.getOrDefault(vo.getScope(), "全场通用"));
            vo.setRuleText(strategyFactory.describe(probe(vo)));

            int mine = receivedMap.getOrDefault(vo.getId(), 0);
            vo.setReceivedByMe(mine);

            int remain = vo.getRemainCount() == null ? 0 : vo.getRemainCount();
            int perLimit = vo.getPerLimit() == null ? 1 : vo.getPerLimit();
            // 先判「领完了」还是先判「你领满了」不影响结果，但文案对用户更友好：
            // 券已经发光时告诉他还剩多少张已经没意义了
            if (remain <= 0) {
                vo.setCanReceive(false);
                vo.setBlockReason("已抢光");
            } else if (mine >= perLimit) {
                vo.setCanReceive(false);
                vo.setBlockReason("每人限领 " + perLimit + " 张，你已领满");
            } else {
                vo.setCanReceive(true);
                vo.setBlockReason(null);
            }
            list.add(vo);
        }
        return PageResult.of(result, list);
    }

    /* ==================================================================
     *  领取
     * ================================================================== */

    @Override
    public void receive(Long couponId) {
        Long userId = UserContext.userId();
        Coupon coupon = couponMapper.selectById(couponId);
        BusinessException.throwIf(coupon == null
                        || !Integer.valueOf(Constants.STATUS_ENABLED).equals(coupon.getStatus()),
                ErrorCode.COUPON_NOT_FOUND);

        // 快速失败：真正的并发防护在下面加锁之后
        LocalDateTime now = LocalDateTime.now();
        BusinessException.throwIf(coupon.getStartTime() != null && now.isBefore(coupon.getStartTime()),
                ErrorCode.COUPON_NOT_FOUND, "优惠券尚未开始发放");
        BusinessException.throwIf(coupon.getEndTime() != null && !now.isBefore(coupon.getEndTime()),
                ErrorCode.COUPON_EXPIRED);
        BusinessException.throwIf(remainOf(coupon) <= 0, ErrorCode.COUPON_RECEIVED_OUT);
        BusinessException.throwIf(userCouponMapper.countByUserAndCoupon(userId, couponId) >= perLimitOf(coupon),
                ErrorCode.COUPON_RECEIVE_LIMIT);

        // 锁按「券 + 用户」粒度：同一张券的不同用户互不阻塞，只有同一个人重复点才排队
        String lockKey = Constants.KEY_COUPON_RECEIVE_LOCK + couponId + ":" + userId;
        String requestId = UUID.randomUUID().toString();
        if (!redisLockUtil.lock(lockKey, requestId, RECEIVE_LOCK_WAIT_MILLIS, RECEIVE_LOCK_LEASE_MILLIS)) {
            throw new BusinessException(ErrorCode.REPEAT_SUBMIT, "领取过于频繁，请稍后再试");
        }
        try {
            // 锁在事务外面：先抢到锁，再开事务；事务提交后才释放锁。
            // 若把 db 操作直接写在 @Transactional 方法里，锁会在事务提交前就释放，
            // 后一个请求可能读到「还没提交」的旧数据，限领判断就会失效。
            txTemplate.executeWithoutResult(status -> doReceive(userId, coupon));
        } finally {
            redisLockUtil.unlock(lockKey, requestId);
        }
    }

    /** 领取的数据库部分，必须运行在事务内 */
    private void doReceive(Long userId, Coupon coupon) {
        // 进临界区后再查一次：外面那次检查只为快速失败，这一次才是权威判断
        int mine = userCouponMapper.countByUserAndCoupon(userId, coupon.getId());
        BusinessException.throwIf(mine >= perLimitOf(coupon), ErrorCode.COUPON_RECEIVE_LIMIT);

        // 原子扣减发放数量：返回 0 行 = 已被别人抢完
        int rows = couponMapper.increaseReceived(coupon.getId());
        BusinessException.throwIf(rows == 0, ErrorCode.COUPON_RECEIVED_OUT);

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(coupon.getId());
        uc.setStatus(Constants.USER_COUPON_UNUSED);
        // 过期时间冗余一份：之后判断「还能不能用」不必再联 coupon 表
        uc.setExpireTime(coupon.getEndTime());
        userCouponMapper.insert(uc);

        // 站内通知（内部已吞异常，不会影响本次领取）
        messageService.send(userId, Constants.MSG_COUPON, "优惠券到账",
                "「" + coupon.getName() + "」已放入你的卡包，"
                        + strategyFactory.describe(coupon)
                        + (coupon.getEndTime() == null ? "" : "，有效期至 " + DATE_FMT.format(coupon.getEndTime())),
                uc.getId());

        log.info("[优惠券] 用户 {} 领取券 {}（{}），已领 {}/{} 张",
                userId, coupon.getId(), coupon.getName(), mine + 1, perLimitOf(coupon));
    }

    /* ==================================================================
     *  我的优惠券
     * ================================================================== */

    @Override
    public PageResult<UserCouponVO> mine(CouponQueryDTO query) {
        query.normalize();
        Long userId = UserContext.userId();
        Page<UserCouponVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<UserCouponVO> result = userCouponMapper.selectMinePage(page, userId, query.getStatus());
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result, result.getRecords());
    }

    @Override
    public Map<Integer, Integer> myCount() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(Constants.USER_COUPON_UNUSED, 0);
        map.put(Constants.USER_COUPON_USED, 0);
        map.put(Constants.USER_COUPON_EXPIRED, 0);
        for (Map<String, Object> row : userCouponMapper.countGroupByStatus(UserContext.userId())) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            if (status instanceof Number s && cnt instanceof Number c) {
                map.put(s.intValue(), c.intValue());
            }
        }
        return map;
    }

    /* ==================================================================
     *  结算页可用券
     * ================================================================== */

    @Override
    public CouponAvailableResultVO available(BigDecimal amount, Integer scope) {
        Long userId = UserContext.userId();
        BigDecimal orderAmount = scale(amount);

        List<UserCouponVO> usable = userCouponMapper.selectUsable(userId, LocalDateTime.now());

        List<CouponAvailableVO> list = new ArrayList<>(usable.size());
        for (UserCouponVO uc : usable) {
            // 适用范围不匹配的券根本不出现（不是「灰掉」而是「跟这次消费无关」）
            if (!scopeMatches(uc.getScope(), scope)) {
                continue;
            }
            Coupon probe = probe(uc);
            boolean meet = strategyFactory.meetThreshold(probe, orderAmount);
            BigDecimal discount = meet
                    ? strategyFactory.calculateDiscount(probe, orderAmount)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            CouponAvailableVO vo = new CouponAvailableVO();
            vo.setUserCouponId(uc.getId());
            vo.setCouponId(uc.getCouponId());
            vo.setName(uc.getName());
            vo.setType(uc.getType());
            vo.setTypeDesc(TYPE_DESC.getOrDefault(uc.getType(), "优惠券"));
            vo.setRuleText(strategyFactory.describe(probe));
            vo.setThreshold(uc.getThreshold());
            vo.setDiscountAmount(discount);
            vo.setPayAmount(scale(orderAmount.subtract(discount)));
            vo.setUsable(meet);
            vo.setBlockReason(meet ? null : thresholdTip(uc.getThreshold(), orderAmount));
            vo.setExpireTime(uc.getExpireTime());
            list.add(vo);
        }

        // 可用的排前面，再按抵扣金额从高到低 —— 用户最关心「哪张最划算」
        list.sort(Comparator.comparing(CouponAvailableVO::getUsable).reversed()
                .thenComparing(CouponAvailableVO::getDiscountAmount, Comparator.reverseOrder()));

        CouponAvailableResultVO result = new CouponAvailableResultVO();
        result.setAmount(orderAmount);
        result.setList(list);

        int availableCount = (int) list.stream().filter(v -> Boolean.TRUE.equals(v.getUsable())).count();
        result.setAvailableCount(availableCount);
        if (availableCount > 0) {
            CouponAvailableVO best = list.get(0);
            result.setBestUserCouponId(best.getUserCouponId());
            result.setBestDiscountAmount(best.getDiscountAmount());
            result.setBestPayAmount(best.getPayAmount());
        } else {
            result.setBestDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            result.setBestPayAmount(orderAmount);
        }
        return result;
    }

    /* ==================================================================
     *  下单核销 / 归还
     * ================================================================== */

    @Override
    public BigDecimal previewDiscount(Long userCouponId, BigDecimal amount, Integer scope) {
        // 与下单核销共用同一段校验：预览与下单结论必须一致，
        // 否则会出现「结算页说能减 30、提交时告诉你不满足门槛」
        return validateAndCalculate(userCouponId, amount, scope, UserContext.userId()).getDiscountAmount();
    }

    @Override
    public CouponUseResultVO useForOrder(Long userCouponId, BigDecimal amount, Integer scope, String orderNo) {
        Long userId = UserContext.userId();
        CouponUseResultVO result = validateAndCalculate(userCouponId, amount, scope, userId);

        // 条件更新：只有「当前确实未使用」才置为已使用。
        // 本方法运行在下单事务里，这一行的行锁会一直持有到订单落库提交，
        // 因此并发的两笔订单不可能同时用掉同一张券。
        int rows = userCouponMapper.useCoupon(userCouponId, userId, orderNo);
        BusinessException.throwIf(rows == 0,
                ErrorCode.COUPON_USED, "优惠券已被使用，请重新选择");

        log.info("[优惠券] 用户 {} 订单 {} 使用券 {}（{}），抵扣 {} 元",
                userId, orderNo, result.getCouponId(), result.getName(), result.getDiscountAmount());
        return result;
    }

    /**
     * 券的校验与试算（预览与核销共用）
     *
     * <p>把这段逻辑抽出来只有一个目的：<b>保证「预览」与「下单」用的是同一套判断</b>。
     * 两处各写一遍的话，任何一次规则调整都可能只改一边。</p>
     */
    private CouponUseResultVO validateAndCalculate(Long userCouponId, BigDecimal amount,
                                                   Integer scope, Long userId) {
        UserCouponVO vo = userCouponMapper.selectVoById(userCouponId);
        BusinessException.throwIf(vo == null, ErrorCode.COUPON_NOT_FOUND);
        BusinessException.throwIf(!userId.equals(vo.getUserId()),
                ErrorCode.NO_PERMISSION, "不能使用他人的优惠券");

        if (Integer.valueOf(Constants.USER_COUPON_USED).equals(vo.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_USED);
        }
        if (Integer.valueOf(Constants.USER_COUPON_EXPIRED).equals(vo.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        BusinessException.throwIf(vo.getExpireTime() != null && !now.isBefore(vo.getExpireTime()),
                ErrorCode.COUPON_EXPIRED);
        BusinessException.throwIf(!scopeMatches(vo.getScope(), scope),
                ErrorCode.COUPON_NOT_AVAILABLE,
                "该优惠券仅限" + SCOPE_DESC.getOrDefault(vo.getScope(), "特定") + "使用");

        Coupon probe = probe(vo);
        BigDecimal orderAmount = scale(amount);
        BusinessException.throwIf(!strategyFactory.meetThreshold(probe, orderAmount),
                ErrorCode.COUPON_THRESHOLD_NOT_ENOUGH,
                "订单金额未达到 " + plain(vo.getThreshold()) + " 元，暂不可用该券");

        CouponUseResultVO result = new CouponUseResultVO();
        result.setUserCouponId(vo.getId());
        result.setCouponId(vo.getCouponId());
        result.setName(vo.getName());
        result.setRuleText(strategyFactory.describe(probe));
        result.setDiscountAmount(strategyFactory.calculateDiscount(probe, orderAmount));
        return result;
    }

    @Override
    public void giveBackByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        int rows = userCouponMapper.returnByOrderNo(orderNo);
        if (rows > 0) {
            log.info("[优惠券] 订单 {} 取消/退款，优惠券已归还用户", orderNo);
        }
    }

    /* ==================================================================
     *  定时任务
     * ================================================================== */

    @Override
    public int markExpired() {
        int rows = userCouponMapper.markExpired(LocalDateTime.now());
        if (rows > 0) {
            log.info("[定时任务] 优惠券过期标记完成：{} 张", rows);
        }
        return rows;
    }

    @Override
    public int sendExpireReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(EXPIRE_REMIND_HOURS);
        List<UserCoupon> list = userCouponMapper.selectExpiringSoon(now, deadline);
        if (list.isEmpty()) {
            return 0;
        }

        int sent = 0;
        for (UserCoupon uc : list) {
            String title = "优惠券即将过期";
            // 幂等：同一张用户券只提醒一次。这里必须带标题一起判断，
            // 否则「券到账」那条消息会把过期提醒挡掉（两者 related_id 相同）
            if (messageService.alreadySent(uc.getUserId(), Constants.MSG_COUPON, uc.getId(), title)) {
                continue;
            }
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            String name = coupon == null ? "优惠券" : coupon.getName();
            messageService.send(uc.getUserId(), Constants.MSG_COUPON, title,
                    "「" + name + "」将在 " + DATE_FMT.format(uc.getExpireTime()) + " 过期，记得使用",
                    uc.getId());
            sent++;
        }
        if (sent > 0) {
            log.info("[定时任务] 优惠券过期提醒完成：{} 条", sent);
        }
        return sent;
    }

    /* ==================== 内部方法 ==================== */

    /** 我的各券种已领张数：couponId → 张数 */
    private Map<Long, Integer> receivedCountMap(Long userId) {
        Map<Long, Integer> map = new HashMap<>();
        for (Map<String, Object> row : couponMapper.countReceivedByCoupon(userId)) {
            Object cid = row.get("couponId");
            Object cnt = row.get("cnt");
            if (cid instanceof Number c && cnt instanceof Number n) {
                map.put(c.longValue(), n.intValue());
            }
        }
        return map;
    }

    /**
     * 把 VO 还原成一个「用于计算」的券对象
     *
     * <p>策略接口面向实体 {@link Coupon}，而列表 / 可用券查出来的都是 VO。
     * 与其为了省一次对象拷贝把策略接口改成接受一堆散字段（那样策略就会与 VO 耦合），
     * 不如在这里做一次属性拷贝——字段同名，一行搞定。</p>
     */
    private Coupon probe(Object source) {
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(source, coupon);
        // UserCouponVO 的 id 是「用户券 id」，而策略日志里要的是「券种 id」，这里纠正一下，
        // 否则排查问题时日志会指向一张不存在的券
        if (source instanceof UserCouponVO uc) {
            coupon.setId(uc.getCouponId());
        }
        return coupon;
    }

    /** 给「我的优惠券」补齐描述性字段 */
    private void decorate(UserCouponVO vo) {
        vo.setTypeDesc(TYPE_DESC.getOrDefault(vo.getType(), "优惠券"));
        vo.setScopeDesc(SCOPE_DESC.getOrDefault(vo.getScope(), "全场通用"));
        vo.setStatusDesc(STATUS_DESC.getOrDefault(vo.getStatus(), "未知"));
        vo.setRuleText(strategyFactory.describe(probe(vo)));
    }

    /**
     * 券的适用范围与本次消费类型是否匹配
     *
     * @param couponScope 券的 scope：1全场 2仅商品 3仅课程
     * @param targetScope 本次消费类型：{@code Constants.TARGET_GOODS} 或 {@code TARGET_COURSE}；
     *                    为 null 时只有全场通用券能匹配
     */
    private boolean scopeMatches(Integer couponScope, Integer targetScope) {
        if (couponScope == null || Integer.valueOf(Constants.COUPON_SCOPE_ALL).equals(couponScope)) {
            return true;
        }
        if (targetScope == null) {
            return false;
        }
        if (Integer.valueOf(Constants.COUPON_SCOPE_GOODS).equals(couponScope)) {
            return Integer.valueOf(Constants.TARGET_GOODS).equals(targetScope);
        }
        if (Integer.valueOf(Constants.COUPON_SCOPE_COURSE).equals(couponScope)) {
            return Integer.valueOf(Constants.TARGET_COURSE).equals(targetScope);
        }
        return false;
    }

    /** 门槛没达到时给一句「还差多少」的提示 */
    private String thresholdTip(BigDecimal threshold, BigDecimal amount) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal gap = threshold.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return "再买 " + plain(gap) + " 元可用";
    }

    private int remainOf(Coupon coupon) {
        int total = coupon.getTotalCount() == null ? 0 : coupon.getTotalCount();
        int received = coupon.getReceivedCount() == null ? 0 : coupon.getReceivedCount();
        return total - received;
    }

    private int perLimitOf(Coupon coupon) {
        return coupon.getPerLimit() == null || coupon.getPerLimit() < 1 ? 1 : coupon.getPerLimit();
    }

    private static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }
}
