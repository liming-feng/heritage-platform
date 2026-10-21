package com.lmf.heritage.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.coupon.dto.AdminCouponQueryDTO;
import com.lmf.heritage.module.coupon.dto.AdminUserCouponQueryDTO;
import com.lmf.heritage.module.coupon.dto.CouponSaveDTO;
import com.lmf.heritage.module.coupon.entity.Coupon;
import com.lmf.heritage.module.coupon.mapper.CouponMapper;
import com.lmf.heritage.module.coupon.mapper.UserCouponMapper;
import com.lmf.heritage.module.coupon.service.AdminCouponService;
import com.lmf.heritage.module.coupon.vo.AdminCouponVO;
import com.lmf.heritage.module.coupon.vo.AdminUserCouponVO;
import com.lmf.heritage.module.coupon.vo.CouponExcelVO;
import com.lmf.heritage.module.coupon.vo.CouponStatVO;
import com.lmf.heritage.module.coupon.vo.UserCouponExcelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端 · 优惠券运营实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCouponServiceImpl implements AdminCouponService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 导出上限 */
    private static final int EXPORT_LIMIT = 5000;

    /** 默认核销排行条数 */
    private static final int DEFAULT_TOP_N = 10;

    private static final Map<Integer, String> TYPE_DESC = Map.of(
            Constants.COUPON_TYPE_FULL_REDUCTION, "满减券",
            Constants.COUPON_TYPE_DISCOUNT, "折扣券");

    private static final Map<Integer, String> SCOPE_DESC = Map.of(
            Constants.COUPON_SCOPE_ALL, "全场通用",
            Constants.COUPON_SCOPE_GOODS, "仅商品",
            Constants.COUPON_SCOPE_COURSE, "仅课程");

    private static final Map<Integer, String> USER_COUPON_STATUS_DESC = Map.of(
            Constants.USER_COUPON_UNUSED, "未使用",
            Constants.USER_COUPON_USED, "已使用",
            Constants.USER_COUPON_EXPIRED, "已过期");

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    /* ==================== 券种列表与详情 ==================== */

    @Override
    public PageResult<AdminCouponVO> page(AdminCouponQueryDTO query) {
        query.normalize();
        IPage<AdminCouponVO> result = queryPage(query, query.getPageNum(), query.getPageSize());
        return PageResult.of(result, result.getRecords());
    }

    /** 查询 + 装饰（分页与导出共用，理由同资讯/商品管理） */
    private IPage<AdminCouponVO> queryPage(AdminCouponQueryDTO query, Long pageNum, Long pageSize) {
        Page<AdminCouponVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminCouponVO> result = couponMapper.selectAdminCouponPage(page, query);
        result.getRecords().forEach(this::decorate);
        return result;
    }

    @Override
    public AdminCouponVO detail(Long id) {
        AdminCouponVO vo = couponMapper.selectAdminCouponById(id);
        BusinessException.throwIf(vo == null, ErrorCode.COUPON_NOT_FOUND);
        decorate(vo);
        return vo;
    }

    /* ==================== 创建 / 编辑 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(CouponSaveDTO dto) {
        dto.normalize();
        boolean isNew = dto.getId() == null;

        boolean fullReduction = Integer.valueOf(Constants.COUPON_TYPE_FULL_REDUCTION).equals(dto.getType());
        boolean discount = Integer.valueOf(Constants.COUPON_TYPE_DISCOUNT).equals(dto.getType());
        BusinessException.throwIf(!fullReduction && !discount,
                ErrorCode.PARAM_ERROR, "type 只能是 1（满减）或 2（折扣）");

        // 各类型必填项：留到下单时才报「券规则不完整」就来不及了
        BusinessException.throwIf(fullReduction && (dto.getAmount() == null
                        || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0),
                ErrorCode.PARAM_ERROR, "满减券必须填写大于 0 的减免金额");
        BusinessException.throwIf(discount && (dto.getDiscount() == null
                        || dto.getDiscount().compareTo(BigDecimal.ZERO) <= 0
                        || dto.getDiscount().compareTo(BigDecimal.ONE) > 0),
                ErrorCode.PARAM_ERROR, "折扣券的折扣率必须在 0 到 1 之间（如 0.85 表示 85 折）");
        BusinessException.throwIf(dto.getTotalCount() == null || dto.getTotalCount() <= 0,
                ErrorCode.PARAM_ERROR, "发放总量必须是大于 0 的整数");
        BusinessException.throwIf(dto.getPerLimit() != null && dto.getPerLimit() <= 0,
                ErrorCode.PARAM_ERROR, "每人限领张数必须大于 0");
        if (dto.getScope() != null) {
            BusinessException.throwIf(dto.getScope() < Constants.COUPON_SCOPE_ALL
                            || dto.getScope() > Constants.COUPON_SCOPE_COURSE,
                    ErrorCode.PARAM_ERROR, "scope 只能是 1全场通用 2仅商品 3仅课程");
        }
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            BusinessException.throwIf(dto.getEndTime().isBefore(dto.getStartTime()),
                    ErrorCode.PARAM_ERROR, "有效期结束时间不能早于开始时间");
        }

        Coupon coupon;
        if (isNew) {
            coupon = new Coupon();
            coupon.setReceivedCount(0);
            coupon.setPerLimit(dto.getPerLimit() == null ? 1 : dto.getPerLimit());
            coupon.setScope(dto.getScope() == null ? Constants.COUPON_SCOPE_ALL : dto.getScope());
            coupon.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.STATUS_ENABLED);
        } else {
            coupon = couponMapper.selectById(dto.getId());
            BusinessException.throwIf(coupon == null, ErrorCode.COUPON_NOT_FOUND);
            if (dto.getPerLimit() != null) {
                coupon.setPerLimit(dto.getPerLimit());
            }
            if (dto.getScope() != null) {
                coupon.setScope(dto.getScope());
            }
            if (dto.getStatus() != null) {
                coupon.setStatus(dto.getStatus());
            }
        }

        // 发放总量只能往上调：调小会让「已领取 > 发放总量」，剩余量变负数、按钮状态全部失真
        if (!isNew && dto.getTotalCount() < coupon.getReceivedCount()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "发放总量不能小于已领取数量（" + coupon.getReceivedCount() + " 张），如需收紧请改用「停用」");
        }
        coupon.setTotalCount(dto.getTotalCount());

        coupon.setName(dto.getName());
        coupon.setType(dto.getType());
        coupon.setThreshold(dto.getThreshold() == null ? BigDecimal.ZERO : dto.getThreshold());
        // 清掉不属于当前类型的字段：避免「折扣券上还挂着一个满减金额」，
        // 这种残留会让之后每一次计算都要先判断该用哪个字段
        coupon.setAmount(fullReduction ? dto.getAmount() : null);
        coupon.setDiscount(discount ? dto.getDiscount() : null);
        coupon.setStartTime(dto.getStartTime());
        coupon.setEndTime(dto.getEndTime());
        coupon.setDescription(dto.getDescription());

        if (isNew) {
            couponMapper.insert(coupon);
        } else {
            couponMapper.updateById(coupon);
        }
        log.info("[优惠券管理] {}券种 #{}（{}）type={} total={} perLimit={} scope={}",
                isNew ? "新建" : "编辑", coupon.getId(), coupon.getName(),
                coupon.getType(), coupon.getTotalCount(), coupon.getPerLimit(), coupon.getScope());
        return coupon.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        BusinessException.throwIf(status == null
                        || (status != Constants.STATUS_DISABLED && status != Constants.STATUS_ENABLED),
                ErrorCode.PARAM_ERROR, "status 只能是 0（停用）或 1（启用）");
        Coupon coupon = couponMapper.selectById(id);
        BusinessException.throwIf(coupon == null, ErrorCode.COUPON_NOT_FOUND);

        coupon.setStatus(status);
        couponMapper.updateById(coupon);
        log.info("[优惠券管理] 券种 #{}（{}）{}", id, coupon.getName(),
                status == Constants.STATUS_ENABLED ? "启用" : "停用");
    }

    /* ==================== 统计 ==================== */

    @Override
    public CouponStatVO stat(Integer topN) {
        int n = (topN == null || topN <= 0) ? DEFAULT_TOP_N : Math.min(topN, 50);

        Map<String, Object> couponOverall = couponMapper.statOverall();
        Map<String, Object> recordOverall = userCouponMapper.statOverall();

        Long couponTotal = toLong(couponOverall.get("couponTotal"));
        Long activeCoupon = toLong(couponOverall.get("activeCoupon"));
        Long issuedTotal = toLong(couponOverall.get("issuedTotal"));
        Long receivedTotal = toLong(couponOverall.get("receivedTotal"));

        Long receiveTotal = toLong(recordOverall.get("receiveTotal"));
        Long unusedTotal = toLong(recordOverall.get("unusedTotal"));
        Long usedTotal = toLong(recordOverall.get("usedTotal"));
        Long expiredTotal = toLong(recordOverall.get("expiredTotal"));

        CouponStatVO vo = new CouponStatVO();
        vo.setCouponTotal(couponTotal);
        vo.setActiveCoupon(activeCoupon);
        vo.setIssuedTotal(issuedTotal);
        vo.setReceivedTotal(receivedTotal);
        vo.setUnusedTotal(unusedTotal);
        vo.setUsedTotal(usedTotal);
        vo.setExpiredTotal(expiredTotal);
        // 分母取「已领取」而不是「发放总量」：没人领的券不该拉低核销率
        vo.setReceiveRate(rate(receivedTotal, issuedTotal));
        vo.setUseRate(rate(usedTotal, receivedTotal));

        List<CouponStatVO.TypeStat> typeStats = new ArrayList<>();
        for (Map<String, Object> row : couponMapper.statByType()) {
            Integer type = ((Number) row.get("type")).intValue();
            Long received = toLong(row.get("receivedCount"));
            Long used = toLong(row.get("usedCount"));
            CouponStatVO.TypeStat ts = new CouponStatVO.TypeStat();
            ts.setType(type);
            ts.setTypeDesc(TYPE_DESC.getOrDefault(type, "未知"));
            ts.setCouponCount(toLong(row.get("couponCount")));
            ts.setReceivedCount(received);
            ts.setUsedCount(used);
            ts.setUseRate(rate(used, received));
            typeStats.add(ts);
        }
        vo.setTypeStats(typeStats);

        // 核销排行：直接用券种列表的数据排序，不额外写一条 SQL
        // —— 排行与列表必须是同一口径，否则「榜上第 3 名」点进去看到的数字对不上
        AdminCouponQueryDTO all = new AdminCouponQueryDTO();
        all.setPageNum(1L);
        all.setPageSize((long) EXPORT_LIMIT);
        List<CouponStatVO.CouponRank> ranks = queryPage(all, 1L, (long) EXPORT_LIMIT).getRecords().stream()
                .filter(v -> v.getReceivedCount() != null && v.getReceivedCount() > 0)
                .map(v -> {
                    CouponStatVO.CouponRank r = new CouponStatVO.CouponRank();
                    r.setCouponId(v.getId());
                    r.setCouponName(v.getName());
                    r.setTypeDesc(v.getTypeDesc());
                    r.setReceivedCount(v.getReceivedCount().longValue());
                    r.setUsedCount(v.getUsedCount() == null ? 0L : v.getUsedCount().longValue());
                    r.setUseRate(v.getUseRate());
                    return r;
                })
                .sorted(Comparator
                        .comparing(CouponStatVO.CouponRank::getUseRate,
                                Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder()))
                        .thenComparing(CouponStatVO.CouponRank::getUsedCount,
                                Comparator.<Long>reverseOrder()))
                .limit(n)
                .collect(Collectors.toList());
        vo.setUseTop(ranks);
        return vo;
    }

    /* ==================== 领取记录 ==================== */

    @Override
    public PageResult<AdminUserCouponVO> receivePage(AdminUserCouponQueryDTO query) {
        query.normalize();
        IPage<AdminUserCouponVO> result = queryReceivePage(query, query.getPageNum(), query.getPageSize());
        return PageResult.of(result, result.getRecords());
    }

    private IPage<AdminUserCouponVO> queryReceivePage(AdminUserCouponQueryDTO query, Long pageNum, Long pageSize) {
        Page<AdminUserCouponVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminUserCouponVO> result = userCouponMapper.selectAdminReceivePage(page, query);
        result.getRecords().forEach(this::decorateReceive);
        return result;
    }

    /* ==================== 导出 ==================== */

    @Override
    public List<CouponExcelVO> exportList(AdminCouponQueryDTO query) {
        query.normalize();
        List<AdminCouponVO> records = queryPage(query, 1L, (long) EXPORT_LIMIT).getRecords();
        return records.stream().map(v -> {
            CouponExcelVO e = new CouponExcelVO();
            e.setId(v.getId());
            e.setName(v.getName());
            e.setTypeDesc(v.getTypeDesc());
            e.setScopeDesc(v.getScopeDesc());
            e.setRuleText(v.getRuleText());
            e.setThreshold(v.getThreshold());
            e.setTotalCount(v.getTotalCount());
            e.setReceivedCount(v.getReceivedCount());
            e.setUsedCount(v.getUsedCount());
            e.setReceiveRate(v.getReceiveRate());
            e.setUseRate(v.getUseRate());
            e.setPerLimit(v.getPerLimit());
            e.setStatusDesc(v.getStatusDesc());
            e.setEffectiveDesc(v.getEffectiveDesc());
            e.setStartTimeText(v.getStartTime() == null ? null : v.getStartTime().format(DT));
            e.setEndTimeText(v.getEndTime() == null ? null : v.getEndTime().format(DT));
            return e;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserCouponExcelVO> exportReceiveList(AdminUserCouponQueryDTO query) {
        query.normalize();
        List<AdminUserCouponVO> records =
                queryReceivePage(query, 1L, (long) EXPORT_LIMIT).getRecords();
        return records.stream().map(v -> {
            UserCouponExcelVO e = new UserCouponExcelVO();
            e.setId(v.getId());
            e.setCouponName(v.getCouponName());
            e.setTypeDesc(v.getTypeDesc());
            e.setRuleText(v.getRuleText());
            e.setUsername(v.getUsername());
            e.setNickname(v.getNickname());
            e.setStatusDesc(v.getStatusDesc());
            e.setOrderNo(v.getOrderNo());
            e.setCreateTimeText(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            e.setUsedTimeText(v.getUsedTime() == null ? null : v.getUsedTime().format(DT));
            e.setExpireTimeText(v.getExpireTime() == null ? null : v.getExpireTime().format(DT));
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== 内部工具 ==================== */

    private void decorate(AdminCouponVO vo) {
        vo.setTypeDesc(TYPE_DESC.getOrDefault(vo.getType(), "未知"));
        vo.setScopeDesc(SCOPE_DESC.getOrDefault(vo.getScope(), "全场通用"));
        vo.setStatusDesc(Integer.valueOf(Constants.STATUS_ENABLED).equals(vo.getStatus()) ? "启用" : "停用");
        vo.setRuleText(ruleText(vo));
        vo.setEffectiveDesc(effectiveDesc(vo.getStartTime(), vo.getEndTime()));
        vo.setReceiveRate(rate(vo.getReceivedCount() == null ? 0L : vo.getReceivedCount().longValue(),
                vo.getTotalCount() == null ? 0L : vo.getTotalCount().longValue()));
        vo.setUseRate(rate(vo.getUsedCount() == null ? 0L : vo.getUsedCount().longValue(),
                vo.getReceivedCount() == null ? 0L : vo.getReceivedCount().longValue()));
    }

    private void decorateReceive(AdminUserCouponVO vo) {
        vo.setTypeDesc(TYPE_DESC.getOrDefault(vo.getType(), "未知"));
        vo.setScopeDesc(SCOPE_DESC.getOrDefault(vo.getScope(), "全场通用"));
        vo.setStatusDesc(USER_COUPON_STATUS_DESC.getOrDefault(vo.getStatus(), "未知"));
        vo.setRuleText(ruleText(vo.getType(), vo.getThreshold(), vo.getAmount(), vo.getDiscount()));
    }

    /** 优惠规则文案：把「类型 + 门槛 + 力度」压缩成人能直接读的一句话 */
    private static String ruleText(Integer type, BigDecimal threshold, BigDecimal amount, BigDecimal discount) {
        boolean hasThreshold = threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0;
        if (Integer.valueOf(Constants.COUPON_TYPE_DISCOUNT).equals(type)) {
            if (discount == null) {
                return "折扣券";
            }
            // 0.85 → 8.5 折：乘 10 再去尾零，避免出现「8.500000 折」
            BigDecimal rate = discount.multiply(BigDecimal.TEN).stripTrailingZeros();
            return hasThreshold
                    ? "满 " + plain(threshold) + " 可用，" + rate.toPlainString() + " 折"
                    : "无门槛，" + rate.toPlainString() + " 折";
        }
        if (Integer.valueOf(Constants.COUPON_TYPE_FULL_REDUCTION).equals(type)) {
            if (amount == null) {
                return "满减券";
            }
            return hasThreshold
                    ? "满 " + plain(threshold) + " 减 " + plain(amount) + " 元"
                    : "无门槛减 " + plain(amount) + " 元";
        }
        return "优惠券";
    }

    private static String ruleText(AdminCouponVO vo) {
        return ruleText(vo.getType(), vo.getThreshold(), vo.getAmount(), vo.getDiscount());
    }

    /** 有效期状态：解释「这张券为什么在领券中心看不到」 */
    private static String effectiveDesc(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (start == null && end == null) {
            return "长期有效";
        }
        if (start != null && start.isAfter(now)) {
            return "未开始";
        }
        if (end != null && !end.isAfter(now)) {
            return "已结束";
        }
        return "进行中";
    }

    /** 百分比，分母为 0 时返回 null（而不是 0%，那会被误读成「完全没发出去」） */
    private static BigDecimal rate(Long numerator, Long denominator) {
        if (denominator == null || denominator <= 0 || numerator == null) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    /**
     * MySQL 的 COUNT / SUM 在不同版本下可能返回 Long 或 BigDecimal，
     * 统一按 Number 取值，避免 {@code ClassCastException} 只在某台机器上复现。
     */
    private static Long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
    }
}
