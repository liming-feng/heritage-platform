package com.lmf.heritage.module.booking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.booking.dto.AdminBookingQueryDTO;
import com.lmf.heritage.module.booking.enums.BookingStatusEnum;
import com.lmf.heritage.module.booking.mapper.BookingMapper;
import com.lmf.heritage.module.booking.service.AdminBookingService;
import com.lmf.heritage.module.booking.service.BookingConfigService;
import com.lmf.heritage.module.booking.service.BookingStatusLogService;
import com.lmf.heritage.module.booking.vo.AdminBookingDetailVO;
import com.lmf.heritage.module.booking.vo.AdminBookingExcelVO;
import com.lmf.heritage.module.booking.vo.AdminBookingStatVO;
import com.lmf.heritage.module.booking.vo.BookingStatusLogVO;
import com.lmf.heritage.module.booking.vo.BookingVO;
import com.lmf.heritage.module.course.enums.CourseStatusEnum;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端 · 预约监管与统计实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingServiceImpl implements AdminBookingService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_TEXT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int DEFAULT_TREND_DAYS = 30;
    private static final int MAX_TREND_DAYS = 90;
    private static final int DEFAULT_TOP = 10;
    private static final int MAX_TOP = 50;
    private static final int EXPORT_LIMIT = 5000;

    private static final String[] WEEK_CN = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /** 四个时段固定顺序，用于把 SQL 的结果补齐成固定 4 根柱子 */
    private static final String[][] SLOTS = {
            {"morning", "上午", "06:00-12:00"},
            {"afternoon", "下午", "12:00-18:00"},
            {"evening", "晚上", "18:00-24:00"},
            {"night", "凌晨", "00:00-06:00"},
    };

    private final BookingMapper bookingMapper;
    private final BookingStatusLogService bookingStatusLogService;
    private final BookingConfigService configService;
    private final UserMapper userMapper;

    /* ==================== 列表与详情 ==================== */

    @Override
    public PageResult<BookingVO> page(AdminBookingQueryDTO query) {
        query.normalize();
        Integer timeoutHours = configService.getInt(Constants.CFG_AUDIT_TIMEOUT_HOURS, 24);
        IPage<BookingVO> page = bookingMapper.selectAdminBookingPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, timeoutHours);
        page.getRecords().forEach(this::decorate);
        return PageResult.of(page, page.getRecords());
    }

    @Override
    public AdminBookingDetailVO detail(Long bookingId) {
        BookingVO booking = bookingMapper.selectVoById(bookingId);
        BusinessException.throwIf(booking == null, ErrorCode.BOOKING_NOT_FOUND);
        decorate(booking);

        AdminBookingDetailVO vo = new AdminBookingDetailVO();
        vo.setBooking(booking);
        vo.setAuditorName(auditorName(bookingId));
        List<BookingStatusLogVO> logs = bookingStatusLogService.listByBooking(bookingId);
        vo.setStatusLogs(logs);
        vo.setTrailCount(logs.size());
        return vo;
    }

    @Override
    public List<BookingStatusLogVO> statusLogs(Long bookingId) {
        return bookingStatusLogService.listByBooking(bookingId);
    }

    @Override
    public List<AdminBookingExcelVO> exportList(AdminBookingQueryDTO query) {
        query.normalize();
        query.setPageNum(1L);
        query.setPageSize((long) EXPORT_LIMIT);
        Integer timeoutHours = configService.getInt(Constants.CFG_AUDIT_TIMEOUT_HOURS, 24);
        IPage<BookingVO> page = bookingMapper.selectAdminBookingPage(
                new Page<>(1, EXPORT_LIMIT), query, timeoutHours);
        page.getRecords().forEach(this::decorate);
        return page.getRecords().stream().map(v -> {
            AdminBookingExcelVO e = new AdminBookingExcelVO();
            e.setBookingNo(v.getBookingNo());
            e.setCourseTitle(v.getCourseTitle());
            e.setCraftsmanName(v.getCraftsmanName());
            e.setContactName(v.getContactName());
            e.setContactPhone(v.getContactPhone());
            e.setQuota(v.getQuota());
            e.setStatusDesc(v.getStatusDesc());
            e.setCheckinCode(v.getCheckinCode());
            e.setCourseStartTimeText(v.getCourseStartTime() == null ? null
                    : v.getCourseStartTime().format(DT));
            e.setCreateTimeText(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            e.setRemark(v.getRemark());
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== ① 时段分布 ==================== */

    @Override
    public AdminBookingStatVO.SlotDistribution slotDistribution() {
        Map<String, long[]> bySlot = new LinkedHashMap<>();
        long total = 0;
        long quota = 0;
        for (Map<String, Object> row : bookingMapper.statTimeSlot()) {
            String slot = String.valueOf(row.get("slot"));
            long cnt = num(row.get("bookingCount"));
            long passed = num(row.get("passedCount"));
            bySlot.put(slot, new long[]{cnt, passed});
            total += cnt;
            quota += num(row.get("quotaSum"));
        }

        AdminBookingStatVO.SlotDistribution vo = new AdminBookingStatVO.SlotDistribution();
        vo.setTotal(total);
        vo.setTotalQuota(quota);

        List<AdminBookingStatVO.SlotDistribution.Slot> slots = new ArrayList<>(SLOTS.length);
        for (String[] def : SLOTS) {
            long[] pair = bySlot.getOrDefault(def[0], new long[]{0, 0});
            AdminBookingStatVO.SlotDistribution.Slot s = new AdminBookingStatVO.SlotDistribution.Slot();
            s.setSlot(def[0]);
            s.setSlotName(def[1]);
            s.setRange(def[2]);
            s.setBookingCount(pair[0]);
            s.setPassedCount(pair[1]);
            s.setPercent(percent(pair[0], total));
            slots.add(s);
        }
        vo.setSlots(slots);
        return vo;
    }

    /* ==================== ② 爽约率 ==================== */

    @Override
    public AdminBookingStatVO.AbsentRate absentRate(Integer topN) {
        int n = clamp(topN, 1, MAX_TOP, DEFAULT_TOP);

        long total = 0;
        long attend = 0;
        long absent = 0;
        long endedCourses = 0;
        List<Map<String, Object>> rows = bookingMapper.statAbsentOverall();
        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            total = num(row.get("total"));
            attend = num(row.get("attend"));
            absent = num(row.get("absent"));
            endedCourses = num(row.get("endedCourseCount"));
        }

        AdminBookingStatVO.AbsentRate vo = new AdminBookingStatVO.AbsentRate();
        vo.setTotal(total);
        vo.setAttend(attend);
        vo.setAbsent(absent);
        vo.setEndedCourseCount(endedCourses);
        // 一门已结束的课都没有时返回 null：「还没开过课」和「爽约率 0%」是两件事，
        // 前者不该显示成一个漂亮的零
        vo.setAbsentRate(endedCourses == 0 ? null : percent(absent, total));

        List<AdminBookingStatVO.AbsentRate.CourseAbsent> courses =
                bookingMapper.statAbsentByCourse(n).stream().map(row -> {
                    AdminBookingStatVO.AbsentRate.CourseAbsent c = new AdminBookingStatVO.AbsentRate.CourseAbsent();
                    c.setCourseId(longOf(row.get("courseId")));
                    c.setCourseTitle(str(row.get("courseTitle")));
                    c.setCraftsmanName(str(row.get("craftsmanName")));
                    c.setTotal(num(row.get("total")));
                    c.setAttend(num(row.get("attend")));
                    c.setAbsent(num(row.get("absent")));
                    c.setAbsentRate(percent(num(row.get("absent")), num(row.get("total"))));
                    return c;
                }).toList();
        vo.setCourses(courses);
        return vo;
    }

    /* ==================== ③ 审核转化漏斗 ==================== */

    @Override
    public AdminBookingStatVO.AuditFunnel auditFunnel() {
        // 只查一次：这条 SQL 是一整行聚合，查两次不仅浪费，两次结果理论上还可能不同
        List<Map<String, Object>> rows = bookingMapper.statFunnel();
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);

        long submitted = num(row.get("submitted"));
        long passed = num(row.get("passed"));
        long checkedIn = num(row.get("checkedIn"));

        AdminBookingStatVO.AuditFunnel vo = new AdminBookingStatVO.AuditFunnel();
        vo.setSubmitted(submitted);
        vo.setPassed(passed);
        vo.setCheckedIn(checkedIn);
        vo.setWaitAudit(num(row.get("waitAudit")));
        vo.setRejected(num(row.get("rejected")));
        vo.setCanceled(num(row.get("canceled")));
        vo.setPassRate(percent(passed, submitted));
        vo.setAttendRate(percent(checkedIn, passed));
        vo.setOverallRate(percent(checkedIn, submitted));

        List<AdminBookingStatVO.AuditFunnel.Stage> stages = new ArrayList<>(3);
        stages.add(stage("提交预约", submitted, submitted));
        stages.add(stage("审核通过", passed, submitted));
        stages.add(stage("到场核销", checkedIn, submitted));
        vo.setStages(stages);
        return vo;
    }

    private static AdminBookingStatVO.AuditFunnel.Stage stage(String name, long value, long base) {
        AdminBookingStatVO.AuditFunnel.Stage s = new AdminBookingStatVO.AuditFunnel.Stage();
        s.setName(name);
        s.setValue(value);
        s.setPercent(percent(value, base));
        return s;
    }

    /* ==================== ④ 课程热度榜 ==================== */

    @Override
    public AdminBookingStatVO.CourseHeat courseHeat(Integer topN) {
        int n = clamp(topN, 1, MAX_TOP, DEFAULT_TOP);
        List<AdminBookingStatVO.CourseHeat.Item> list = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : bookingMapper.statCourseHeat(n)) {
            AdminBookingStatVO.CourseHeat.Item it = new AdminBookingStatVO.CourseHeat.Item();
            it.setRank(rank++);
            it.setCourseId(longOf(row.get("courseId")));
            it.setCourseTitle(str(row.get("courseTitle")));
            it.setCourseCover(str(row.get("courseCover")));
            it.setCraftsmanName(str(row.get("craftsmanName")));
            it.setStartTime(dateTimeOf(row.get("startTime")));
            Integer status = intOf(row.get("status"));
            it.setStatus(status);
            it.setStatusDesc(CourseStatusEnum.descOf(status));
            it.setBookingCount(num(row.get("bookingCount")));
            it.setBookingPeople(num(row.get("bookingPeople")));
            it.setTotalQuota(intOf(row.get("totalQuota")));
            it.setRemainQuota(intOf(row.get("remainQuota")));
            it.setUsedRate(usedRate(it.getTotalQuota(), it.getRemainQuota()));
            list.add(it);
        }

        AdminBookingStatVO.CourseHeat vo = new AdminBookingStatVO.CourseHeat();
        vo.setScope("全平台课程");
        vo.setList(list);
        return vo;
    }

    /* ==================== ⑤ 近 N 天趋势 ==================== */

    @Override
    public AdminBookingStatVO.Trend trend(Integer days) {
        int d = clamp(days, 1, MAX_TREND_DAYS, DEFAULT_TREND_DAYS);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(d - 1L);

        Map<String, long[]> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : bookingMapper.statTrend(start.atStartOfDay())) {
            byDay.put(String.valueOf(row.get("statDay")),
                    new long[]{num(row.get("cnt")), num(row.get("passedCnt")), num(row.get("quotaSum"))});
        }

        List<AdminBookingStatVO.Trend.DayPoint> points = new ArrayList<>(d);
        long total = 0;
        long quota = 0;
        long max = 0;
        for (LocalDate cur = start; !cur.isAfter(end); cur = cur.plusDays(1)) {
            String key = cur.format(DAY);
            long[] pair = byDay.getOrDefault(key, new long[]{0, 0, 0});
            AdminBookingStatVO.Trend.DayPoint p = new AdminBookingStatVO.Trend.DayPoint();
            p.setDate(key);
            p.setDateText(cur.format(DAY_TEXT));
            p.setWeekday(WEEK_CN[cur.getDayOfWeek().getValue() - 1]);
            p.setCount(pair[0]);
            p.setPassedCount(pair[1]);
            p.setQuota(pair[2]);
            p.setHasData(pair[0] > 0);
            points.add(p);
            total += pair[0];
            quota += pair[2];
            max = Math.max(max, pair[0]);
        }

        AdminBookingStatVO.Trend vo = new AdminBookingStatVO.Trend();
        vo.setDays(d);
        vo.setStartDate(start.format(DAY));
        vo.setEndDate(end.format(DAY));
        vo.setTotalCount(total);
        vo.setTotalQuota(quota);
        vo.setAvgCount(round1(total * 1.0 / d));
        vo.setMaxCount(max);
        vo.setPoints(points);
        return vo;
    }

    /* ==================== 内部工具 ==================== */

    /**
     * 补全管理端展示用的派生字段
     *
     * <p>这三项（状态文案、取消方文案、是否超时）由用户端/匠人端服务在各自路径上算，
     * 管理端直接复用会让 AdminBookingServiceImpl 依赖 BookingService（那是个大对象，
     * 而且它的方法都带登录身份语义）。派生逻辑本身只有三行，放在这里更清楚。</p>
     */
    private void decorate(BookingVO vo) {
        vo.setStatusDesc(BookingStatusEnum.of(vo.getStatus()).getDesc());
        vo.setCancelByDesc(cancelByText(vo.getCancelBy()));
        Integer timeoutHours = configService.getInt(Constants.CFG_AUDIT_TIMEOUT_HOURS, 24);
        boolean timeout = Integer.valueOf(Constants.BOOKING_WAIT_AUDIT).equals(vo.getStatus())
                && vo.getCreateTime() != null
                && vo.getCreateTime().isBefore(LocalDateTime.now().minusHours(timeoutHours));
        vo.setTimeout(timeout);
    }

    private static String cancelByText(Integer cancelBy) {
        if (cancelBy == null) {
            return null;
        }
        return switch (cancelBy) {
            case Constants.CANCEL_BY_USER -> "用户取消";
            case Constants.CANCEL_BY_CRAFTSMAN -> "匠人取消";
            case Constants.CANCEL_BY_ADMIN -> "管理员取消";
            case Constants.CANCEL_BY_SYSTEM -> "系统自动取消";
            default -> "未知";
        };
    }

    private String auditorName(Long bookingId) {
        // 单独一条查询而不是往 BookingVO 上加 auditorId：那是给前台响应共用的 VO，
        // 多带一个 user id 对用户端毫无用处，只是无谓的信息暴露
        return bookingMapper.selectAuditorName(bookingId);
    }

    private static int clamp(Integer v, int min, int max, int def) {
        if (v == null) {
            return def;
        }
        return Math.max(min, Math.min(max, v));
    }

    private static Double percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return null;
        }
        return round1(numerator * 100.0 / denominator);
    }

    /** 名额使用率：总名额为 0 时返回 null，理由同爽约率 */
    private static Double usedRate(Integer totalQuota, Integer remainQuota) {
        int t = totalQuota == null ? 0 : totalQuota;
        if (t <= 0) {
            return null;
        }
        int r = remainQuota == null ? 0 : remainQuota;
        return round1((t - r) * 100.0 / t);
    }

    private static Double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** MySQL COUNT/SUM 经 JDBC 回来可能是 Long / BigInteger / BigDecimal，一律收敛成 long */
    private static long num(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Long longOf(Object o) {
        return o == null ? null : num(o);
    }

    private static Integer intOf(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static LocalDateTime dateTimeOf(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDateTime t) {
            return t;
        }
        if (o instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return LocalDateTime.parse(o.toString().replace(' ', 'T'));
    }
}
