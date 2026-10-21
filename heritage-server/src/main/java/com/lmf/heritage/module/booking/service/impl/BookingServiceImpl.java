package com.lmf.heritage.module.booking.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.booking.dto.BookingAuditDTO;
import com.lmf.heritage.module.booking.dto.BookingCancelDTO;
import com.lmf.heritage.module.booking.dto.BookingCheckinDTO;
import com.lmf.heritage.module.booking.dto.BookingQueryDTO;
import com.lmf.heritage.module.booking.dto.BookingSubmitDTO;
import com.lmf.heritage.module.booking.entity.Booking;
import com.lmf.heritage.module.booking.enums.BookingStatusEnum;
import com.lmf.heritage.module.booking.mapper.BookingMapper;
import com.lmf.heritage.module.booking.service.BookingConfigService;
import com.lmf.heritage.module.booking.service.BookingService;
import com.lmf.heritage.module.booking.service.BookingStatusLogService;
import com.lmf.heritage.module.booking.vo.BookingVO;
import com.lmf.heritage.module.booking.vo.CheckinResultVO;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.enums.CourseStatusEnum;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.message.service.MessageService;
import com.lmf.heritage.module.review.mapper.ReviewMapper;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.mapper.UserMapper;
import com.lmf.heritage.security.UserContext;
import com.lmf.heritage.util.CraftsmanHelper;
import com.lmf.heritage.util.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 课程预约服务实现 —— <b>整个课题的核心</b>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private final BookingMapper bookingMapper;
    private final CourseMapper courseMapper;
    private final CraftsmanHelper craftsmanHelper;
    private final ReviewMapper reviewMapper;
    private final MessageService messageService;
    private final BookingConfigService configService;
    private final RedisLockUtil redisLockUtil;
    private final TransactionTemplate txTemplate;
    private final UserMapper userMapper;
    private final BookingStatusLogService bookingStatusLogService;

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 抢锁最多等待时间（毫秒）：等不到才快速失败，避免请求长时间挂住。
     * 从配置读取而不是写死——100 并发抢名额时队列能排到几百毫秒，
     * 演示/压测场景可以把 heritage.booking.lock-wait 调大，线上按需收紧。
     */
    @Value("${heritage.booking.lock-wait:3000}")
    private long lockWaitMillis;

    /** 锁自动过期时间（毫秒）：进程崩溃后锁能自己释放，不会永久死锁 */
    @Value("${heritage.booking.lock-lease:10000}")
    private long lockLeaseMillis;

    /** 定时任务单批处理条数 */
    private static final int TASK_BATCH = 200;

    /* ==================================================================================
     *  ★ 预约提交：三层防护（课题题眼，论文核心章节）
     * ==================================================================================
     *
     * 这一块的难点只有一个：**在并发下保证「剩余名额」不被卖超，同时不让同一个人重复预约**。
     * 三层各自解决不同的问题，缺一不可：
     *
     * ┌─ 第一层：Redis 分布式锁 lock:booking:{courseId} ─────────────────────────────────┐
     * │ 【防什么】大量并发请求同时涌向数据库造成的行锁竞争与连接占用。                    │
     * │ 【怎么做】加锁 SET key requestId NX PX 10000 ——                            │
     * │           NX 保证互斥（同一课程同时只有一个请求拿得到）；                        │
     * │           PX 保证进程崩溃后锁能自动过期，不会形成永久死锁。                      │
     * │           释放用 Lua 脚本「先比对 requestId、相同才 DEL」——                   │
     * │           保证「判断 + 删除」的原子性，不会误删「自己超时后别人抢到的锁」。        │
     * │ 【边界】这一层**不负责正确性**，只负责把并发压成串行、减轻数据库压力。             │
     * └───────────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─ 第二层：数据库行锁 + 条件更新 ─────────────────────────────────────────────────┐
     * │ 【防什么】「先查库存、再扣库存」之间的竞态窗口。                                  │
     * │ 【怎么做】① SELECT ... FOR UPDATE 锁住课程行，读到的一定是「前一个事务扣减完」的值；│
     * │           ② UPDATE course SET remain_quota = remain_quota - n                     │
     * │                WHERE id = ? AND remain_quota >= n                                 │
     * │              把「判断」和「扣减」压进同一条 SQL，由 InnoDB 行锁保证原子性，         │
     * │              返回 0 行就代表名额不足，抛异常让整个事务回滚（不会只扣一半）。        │
     * │ 【边界】这才是**正确性的最终保证**：即使第一层的锁完全失效，结果也不会超卖。        │
     * └───────────────────────────────────────────────────────────────────────────────┘
     *
     * ┌─ 第三层：唯一索引 uk_user_course(user_id, course_id) ──────────────────────────┐
     * │ 【防什么】同一个人对同一门课程重复预约（前两层管「够不够」，这层管「是不是同一人」）。│
     * │ 【怎么做】数据库唯一索引是最后一道防线：并发下两个请求都通过了前面的校验时，        │
     * │           只有一个 INSERT 能成功，另一个撞唯一键被拒。                            │
     * │           业务上还要处理「取消后想重新预约」——唯一键对已取消的记录同样生效，      │
     * │           所以不能插新记录，只能把原记录**复活**（见 BookingMapper.revive）。      │
     * └───────────────────────────────────────────────────────────────────────────────┘
     *
     * 【事务边界的处理】锁在事务**外面**获取（用 TransactionTemplate 编程式事务显式控制）：
     *   正确的顺序是「抢到锁 → 开事务 → 提交 → 释放锁」。
     *   如果把锁写在 @Transactional 方法内部，会出现两个问题：
     *   ① 锁在事务提交**之前**就被 finally 释放了，临界区被缩短，语义不严谨；
     *   ② 等锁的线程会一直占着数据库连接不做事，高并发下（本课题验收是 100 并发）
     *      会把连接池耗干，后面的请求直接拿不到连接而失败。
     * ================================================================================== */

    @Override
    public Long submit(BookingSubmitDTO dto) {
        Long userId = UserContext.userId();
        Long courseId = dto.getCourseId();

        // ---------- 阶段一：无锁预校验（快速失败，减少无谓的锁竞争） ----------
        Course course = courseMapper.selectById(courseId);
        BusinessException.throwIf(course == null, ErrorCode.COURSE_NOT_FOUND);
        BusinessException.throwIf(!Integer.valueOf(1).equals(course.getAuditStatus()),
                ErrorCode.COURSE_NOT_FOUND, "课程尚未通过平台审核，暂不可预约");
        validateBookingRules(course, userId);

        // ---------- 阶段二：第一层 Redis 锁，锁内开事务 ----------
        return withCourseLock(courseId, () -> {
            Long bookingId = txTemplate.execute(status -> doSubmit(userId, courseId, dto));
            log.info("[预约] 提交成功 userId={} courseId={} bookingId={}", userId, courseId, bookingId);
            return bookingId;
        });
    }

    /**
     * 锁内的落库逻辑（在事务中执行）
     *
     * <p>顺序：取行锁 → 二次校验 → 原子扣名额 → 复活或插入预约记录 → 发站内消息。</p>
     */
    private Long doSubmit(Long userId, Long courseId, BookingSubmitDTO dto) {
        int quota = dto.getQuota() == null ? 1 : dto.getQuota();

        // ---------- 第二层：数据库行锁 + 原子扣减 ----------
        Course locked = courseMapper.selectForUpdate(courseId);
        BusinessException.throwIf(locked == null, ErrorCode.COURSE_NOT_FOUND);

        int remain = locked.getRemainQuota() == null ? 0 : locked.getRemainQuota();
        BusinessException.throwIf(remain < quota, ErrorCode.BOOKING_QUOTA_NOT_ENOUGH,
                "「" + locked.getTitle() + "」剩余名额不足，当前仅剩 " + remain + " 个");

        // 行锁内再校验一次时限与重复预约：无锁预校验到此刻之间，规则可能已被改动
        validateBookingRules(locked, userId);

        int rows = courseMapper.deductQuota(courseId, quota);
        BusinessException.throwIf(rows == 0, ErrorCode.BOOKING_QUOTA_NOT_ENOUGH,
                "「" + locked.getTitle() + "」剩余名额不足");

        // ---------- 第三层：唯一索引兜底 ----------
        boolean autoPass = !configService.getBoolean(Constants.CFG_AUDIT_REQUIRED, true);
        Booking exist = bookingMapper.selectIncludingDeleted(userId, courseId);

        Long bookingId;
        String checkinCode = null;
        // 复活场景的「原状态」必须在覆盖之前捕获：轨迹里要如实写出
        // 「已取消 → 待审核」，而不是所有记录都从「创建」开始，
        // 否则同一条预约被反复取消重提的轨迹会全糊成一串「创建」
        Integer fromStatus = exist == null ? null : exist.getStatus();
        if (exist != null) {
            // 该用户在这门课上已有记录（已取消 / 已拒绝）：复活它，而不是插新记录
            exist.setBookingNo(generateBookingNo(userId));
            exist.setCraftsmanId(locked.getCraftsmanId());
            exist.setQuota(quota);
            exist.setContactName(dto.getContactName());
            exist.setContactPhone(dto.getContactPhone());
            exist.setRemark(dto.getRemark());
            if (autoPass) {
                checkinCode = generateCheckinCode();
                exist.setStatus(Constants.BOOKING_PASSED);
                exist.setCheckinCode(checkinCode);
                exist.setAuditTime(LocalDateTime.now());
            } else {
                exist.setStatus(Constants.BOOKING_WAIT_AUDIT);
                exist.setCheckinCode(null);
                exist.setAuditTime(null);
            }
            exist.setAuditorId(null);
            bookingMapper.revive(exist);
            bookingId = exist.getId();
            log.info("[预约] 复活已取消/已拒绝的预约记录 bookingId={}", bookingId);
        } else {
            Booking booking = new Booking();
            booking.setBookingNo(generateBookingNo(userId));
            booking.setCourseId(courseId);
            booking.setUserId(userId);
            booking.setCraftsmanId(locked.getCraftsmanId());
            booking.setContactName(dto.getContactName());
            booking.setContactPhone(dto.getContactPhone());
            booking.setQuota(quota);
            booking.setRemark(dto.getRemark());
            if (autoPass) {
                checkinCode = generateCheckinCode();
                booking.setStatus(Constants.BOOKING_PASSED);
                booking.setCheckinCode(checkinCode);
                booking.setAuditTime(LocalDateTime.now());
            } else {
                booking.setStatus(Constants.BOOKING_WAIT_AUDIT);
            }
            try {
                bookingMapper.insert(booking);
            } catch (DuplicateKeyException e) {
                // 唯一索引 uk_user_course 把并发重复提交挡在这里
                log.warn("[预约] 触发唯一索引 uk_user_course，判定为重复预约 userId={} courseId={}", userId, courseId);
                throw new BusinessException(ErrorCode.BOOKING_DUPLICATE);
            }
            bookingId = booking.getId();
        }

        // ---------- 站内消息 ----------
        if (autoPass) {
            messageService.send(userId, Constants.MSG_BOOKING_AUDIT, "预约成功",
                    "你预约的「" + locked.getTitle() + "」无需审核，已直接通过。"
                            + "开课时间 " + fmt(locked.getStartTime()) + "，核销码 " + checkinCode + "。", bookingId);
        } else {
            messageService.send(userId, Constants.MSG_BOOKING_AUDIT, "预约已提交，等待匠人审核",
                    "你预约的「" + locked.getTitle() + "」已提交，匠人审核后会有通知。", bookingId);
        }

        // ---------- 状态流转轨迹 ----------
        bookingStatusLogService.record(bookingId, fromStatus,
                autoPass ? Constants.BOOKING_PASSED : Constants.BOOKING_WAIT_AUDIT,
                autoPass ? "提交预约（该课程已关闭审核，直接通过）" : "提交预约，等待匠人审核");
        return bookingId;
    }

    /**
     * 预约规则的统一校验（配置驱动）
     *
     * <p>被调用两次：一次在拿锁前（快速失败），一次在行锁内（防止校验期间规则被改）。
     * 两处用同一份逻辑，不会出现「哪里漏校验」的问题。</p>
     */
    private void validateBookingRules(Course course, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 1) 课程状态：只有「未开始」能预约
        CourseStatusEnum status = CourseStatusEnum.of(course.getStatus());
        BusinessException.throwIf(status != CourseStatusEnum.NOT_STARTED, ErrorCode.COURSE_ALREADY_ENDED,
                status == CourseStatusEnum.FINISHED ? "课程已结束，无法预约" : "课程已开课，无法预约");

        // 2) 是否超过课程的预约截止时间（没设截止时间则以开课时间为准）
        LocalDateTime deadline = course.getBookingDeadline() != null
                ? course.getBookingDeadline() : course.getStartTime();
        BusinessException.throwIf(deadline != null && now.isAfter(deadline),
                ErrorCode.BOOKING_DEADLINE_PASSED, "该课程的预约已于 " + fmt(deadline) + " 截止");

        // 3) 最少提前预约时长（来自 booking_config，可在管理端调整）
        int advanceHours = configService.getInt(Constants.CFG_BOOKING_ADVANCE_HOURS, 2);
        if (advanceHours > 0 && course.getStartTime() != null) {
            LocalDateTime latest = course.getStartTime().minusHours(advanceHours);
            BusinessException.throwIf(now.isAfter(latest), ErrorCode.BOOKING_ADVANCE_NOT_ENOUGH,
                    "距开课不足 " + advanceHours + " 小时，无法预约");
        }

        // 4) 重复预约：已占着名额（待审核 / 已通过 / 已核销 / 已完成）时不能再次提交
        int occupied = bookingMapper.countOccupied(userId, course.getId());
        BusinessException.throwIf(occupied > 0, ErrorCode.BOOKING_DUPLICATE);

        // 5) 限约次数：配置值在这里的实际含义是「取消 / 被拒后是否允许重新预约」
        //    （表上的唯一索引已经保证一个人一门课至多一条记录，所以能讨论的只有这一种情形）
        Booking exist = bookingMapper.selectIncludingDeleted(userId, course.getId());
        if (exist != null) {
            int maxPerCourse = configService.getInt(Constants.CFG_MAX_BOOKING_PER_COURSE, 1);
            BusinessException.throwIf(maxPerCourse <= 0, ErrorCode.BOOKING_LIMIT_EXCEEDED,
                    "该课程已关闭「取消后重新预约」");
        }
    }

    @Override
    public BookingVO submitAndDetail(BookingSubmitDTO dto) {
        Long bookingId = submit(dto);
        return detail(bookingId);
    }

    /* ==================================================================================
     *  用户端查询
     * ================================================================================== */

    @Override
    public PageResult<BookingVO> myBookings(BookingQueryDTO query) {
        query.normalize();
        Long userId = UserContext.userId();
        IPage<BookingVO> page = bookingMapper.selectBookingPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, userId, null, null);
        return PageResult.of(page, decorate(page.getRecords(), false));
    }

    @Override
    public Map<String, Long> myStatusCount() {
        return toCountMap(bookingMapper.countGroupByStatusForUser(UserContext.userId()));
    }

    @Override
    public BookingVO detail(Long bookingId) {
        Long userId = UserContext.userId();
        Booking booking = bookingMapper.selectById(bookingId);
        BusinessException.throwIf(booking == null, ErrorCode.BOOKING_NOT_FOUND);
        BusinessException.throwIf(!userId.equals(booking.getUserId()), ErrorCode.BOOKING_NOT_BELONG_TO_YOU);

        BookingVO vo = bookingMapper.selectVoById(bookingId);
        BusinessException.throwIf(vo == null, ErrorCode.BOOKING_NOT_FOUND);
        return decorate(List.of(vo), false).get(0);
    }

    @Override
    public String checkinCode(Long bookingId) {
        Booking booking = requireMine(bookingId);
        BusinessException.throwIf(booking.getStatus() != Constants.BOOKING_PASSED
                        && booking.getStatus() != Constants.BOOKING_CHECKED_IN,
                ErrorCode.BOOKING_STATUS_ERROR,
                "预约当前为「" + BookingStatusEnum.of(booking.getStatus()).getDesc() + "」，暂无核销码");
        BusinessException.throwIf(!StringUtils.hasText(booking.getCheckinCode()),
                ErrorCode.BOOKING_STATUS_ERROR, "核销码尚未生成，请稍后再试");
        return booking.getCheckinCode();
    }

    @Override
    public void cancel(BookingCancelDTO dto) {
        Booking booking = requireMine(dto.getBookingId());
        BookingStatusEnum.checkTransfer(booking.getStatus(), BookingStatusEnum.CANCELED);

        Course course = courseMapper.selectById(booking.getCourseId());
        BusinessException.throwIf(course == null, ErrorCode.COURSE_NOT_FOUND);
        String block = userCancelBlockReason(course);
        BusinessException.throwIf(block != null, ErrorCode.BOOKING_CANCEL_DEADLINE_PASSED, block);

        doCancel(booking, Constants.CANCEL_BY_USER, dto.getReason());
        log.info("[预约] 用户 {} 取消预约 {}", booking.getUserId(), booking.getBookingNo());
    }

    /* ==================================================================================
     *  匠人端
     * ================================================================================== */

    @Override
    public PageResult<BookingVO> craftsmanBookings(BookingQueryDTO query) {
        query.normalize();
        Long craftsmanId = currentCraftsmanId();
        Integer timeoutHours = configService.getInt(Constants.CFG_AUDIT_TIMEOUT_HOURS, 24);

        IPage<BookingVO> page = bookingMapper.selectBookingPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query, null, craftsmanId, timeoutHours);
        return PageResult.of(page, decorate(page.getRecords(), true));
    }

    @Override
    public Map<String, Long> craftsmanStatusCount() {
        return toCountMap(bookingMapper.countGroupByStatusForCraftsman(currentCraftsmanId()));
    }

    @Override
    public void audit(BookingAuditDTO dto) {
        Booking booking = requireCraftsmanBooking(dto.getBookingId());
        boolean approve = Boolean.TRUE.equals(dto.getApprove());
        BookingStatusEnum target = approve ? BookingStatusEnum.PASSED : BookingStatusEnum.REJECTED;
        BookingStatusEnum.checkTransfer(booking.getStatus(), target);

        Long auditorId = UserContext.userId();
        if (approve) {
            // 通过：名额在提交时就已预占，这里只生成核销码并推进状态
            String code = generateCheckinCode();
            txTemplate.executeWithoutResult(status -> {
                int rows = bookingMapper.transferStatus(booking.getId(),
                        Constants.BOOKING_WAIT_AUDIT, Constants.BOOKING_PASSED);
                BusinessException.throwIf(rows == 0, ErrorCode.BOOKING_STATUS_ERROR, "预约状态已变化，请刷新后重试");
                bookingMapper.updateCheckin(booking.getId(), code, LocalDateTime.now(), auditorId);
            });
            bookingStatusLogService.record(booking.getId(), Constants.BOOKING_WAIT_AUDIT,
                    Constants.BOOKING_PASSED, "匠人审核通过，核销码 " + code);

            Course course = courseMapper.selectById(booking.getCourseId());
            String title = course == null ? "课程" : course.getTitle();
            messageService.send(booking.getUserId(), Constants.MSG_BOOKING_AUDIT, "预约审核通过",
                    "你预约的「" + title + "」已通过审核，核销码 " + code
                            + "，请于 " + fmt(course == null ? null : course.getStartTime()) + " 到店签到。",
                    booking.getId());
            log.info("[预约] 审核通过 {} 核销码={}", booking.getBookingNo(), code);
        } else {
            BusinessException.throwIf(!StringUtils.hasText(dto.getRejectReason()),
                    ErrorCode.PARAM_ERROR, "拒绝预约时请填写拒绝原因");

            // 拒绝：状态改为已拒绝并归还名额（与状态变更放在同一个事务里）
            doReject(booking, dto.getRejectReason(), auditorId);
            Course course = courseMapper.selectById(booking.getCourseId());
            messageService.send(booking.getUserId(), Constants.MSG_BOOKING_AUDIT, "预约未通过",
                    "你预约的「" + (course == null ? "课程" : course.getTitle()) + "」未通过审核，原因："
                            + dto.getRejectReason() + "。名额已释放，可预约其他课程。", booking.getId());
            log.info("[预约] 审核拒绝 {} 原因={}", booking.getBookingNo(), dto.getRejectReason());
        }
    }

    /**
     * 核销签到
     *
     * <p>错误提示要「具体」——本课题的要求是三种失败情形分别给出不同提示，
     * 便于匠人一眼判断问题出在哪：</p>
     * <ul>
     *   <li>码根本不存在 → 核销码不正确；</li>
     *   <li>码存在但不是自己课的预约 → 该预约不属于你的课程；</li>
     *   <li>已经核销过 → 该预约已核销，请勿重复核销。</li>
     * </ul>
     */
    @Override
    public CheckinResultVO checkin(BookingCheckinDTO dto) {
        Long craftsmanId = currentCraftsmanId();
        // 两个入口（扫码/输入核销码、名单里点核销）在这里收敛成同一条预约记录，
        // 后面的校验与状态流转因此只有一份，不会出现「换个入口就绕过了某条校验」
        Booking booking = resolveCheckinTarget(dto);

        // 先判「是否已核销」，再判归属：重复核销要给出比其他状态错误更明确的提示
        BusinessException.throwIf(Integer.valueOf(Constants.BOOKING_CHECKED_IN).equals(booking.getStatus()),
                ErrorCode.CHECKIN_ALREADY_DONE);
        BusinessException.throwIf(!craftsmanId.equals(booking.getCraftsmanId()),
                ErrorCode.CHECKIN_NOT_YOUR_COURSE);
        // 剩下的状态跳转交给状态机：只有「已通过」能核销
        BookingStatusEnum.checkTransfer(booking.getStatus(), BookingStatusEnum.CHECKED_IN);

        LocalDateTime now = LocalDateTime.now();
        txTemplate.executeWithoutResult(status -> {
            int rows = bookingMapper.transferStatus(booking.getId(),
                    Constants.BOOKING_PASSED, Constants.BOOKING_CHECKED_IN);
            // 并发下两个终端同时扫同一个码，只有一个能成功
            BusinessException.throwIf(rows == 0, ErrorCode.CHECKIN_ALREADY_DONE);
            bookingMapper.updateCheckinTime(booking.getId(), now);
        });
        bookingStatusLogService.record(booking.getId(), Constants.BOOKING_PASSED, Constants.BOOKING_CHECKED_IN,
                dto.getBookingId() != null ? "匠人在名单中手动核销" : "匠人凭核销码核销");

        Course course = courseMapper.selectById(booking.getCourseId());
        CheckinResultVO vo = new CheckinResultVO();
        vo.setBookingId(booking.getId());
        vo.setBookingNo(booking.getBookingNo());
        vo.setCourseTitle(course == null ? null : course.getTitle());
        vo.setCourseAddress(course == null ? null : course.getAddress());
        vo.setCourseStartTime(course == null ? null : course.getStartTime());
        vo.setContactName(booking.getContactName());
        vo.setContactPhone(booking.getContactPhone());
        // 签到台要「一眼确认是谁」，所以把账号昵称与预约时间一并回带，
        // 免得匠人再去后台翻一遍这条预约
        vo.setNickname(nicknameOf(booking.getUserId()));
        vo.setSubmitTime(booking.getCreateTime());
        vo.setQuota(booking.getQuota());
        vo.setCheckinWay(dto.getBookingId() != null
                ? Constants.CHECKIN_WAY_MANUAL : Constants.CHECKIN_WAY_CODE);
        vo.setCheckinTime(now);
        vo.setStatusDesc(BookingStatusEnum.CHECKED_IN.getDesc());

        messageService.send(booking.getUserId(), Constants.MSG_BOOKING_AUDIT, "签到成功",
                "你预约的「" + (course == null ? "课程" : course.getTitle()) + "」已完成签到，祝体验愉快。",
                booking.getId());
        log.info("[预约] 核销成功 {} 匠人 {} 名额 {} 方式={}",
                booking.getBookingNo(), craftsmanId, booking.getQuota(),
                dto.getBookingId() != null ? "名单手动" : "核销码");
        return vo;
    }

    /**
     * 解析核销目标：核销码与预约 id 二选一
     *
     * <p>「码不存在」和「预约不存在」刻意给不同的错误提示：
     * 前者多半是输错了一位数，后者多半是名单数据过期（比如页面停留太久），
     * 匠人在签到台需要据此决定是「让用户再报一次码」还是「刷新名单」。</p>
     */
    private Booking resolveCheckinTarget(BookingCheckinDTO dto) {
        if (dto.getBookingId() != null) {
            Booking byId = bookingMapper.selectById(dto.getBookingId());
            BusinessException.throwIf(byId == null, ErrorCode.BOOKING_NOT_FOUND);
            return byId;
        }
        Booking byCode = bookingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Booking>()
                        .eq(Booking::getCheckinCode, dto.getCheckinCode().trim()).last("LIMIT 1"));
        BusinessException.throwIf(byCode == null, ErrorCode.CHECKIN_CODE_INVALID);
        return byCode;
    }

    /** 预约人昵称（取不到时回落到用户名，再取不到返回 null 由前端显示「非遗用户」） */
    private String nicknameOf(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : user.getUsername();
    }

    @Override
    public void craftsmanCancel(BookingCancelDTO dto) {
        Booking booking = requireCraftsmanBooking(dto.getBookingId());
        BookingStatusEnum.checkTransfer(booking.getStatus(), BookingStatusEnum.CANCELED);
        doCancel(booking, Constants.CANCEL_BY_CRAFTSMAN, dto.getReason());

        Course course = courseMapper.selectById(booking.getCourseId());
        messageService.send(booking.getUserId(), Constants.MSG_BOOKING_AUDIT, "预约已被取消",
                "你预约的「" + (course == null ? "课程" : course.getTitle()) + "」已被匠人取消"
                        + (StringUtils.hasText(dto.getReason()) ? "，原因：" + dto.getReason() : "")
                        + "。名额已释放。", booking.getId());
        log.info("[预约] 匠人取消预约 {}", booking.getBookingNo());
    }

    @Override
    public List<BookingVO> roster(Long courseId) {
        Long craftsmanId = currentCraftsmanId();
        Course course = courseMapper.selectById(courseId);
        BusinessException.throwIf(course == null, ErrorCode.COURSE_NOT_FOUND);
        BusinessException.throwIf(!craftsmanId.equals(course.getCraftsmanId()), ErrorCode.NO_PERMISSION);

        BookingQueryDTO query = new BookingQueryDTO();
        query.setCourseId(courseId);
        query.setPageNum(1L);
        query.setPageSize(100L);
        IPage<BookingVO> page = bookingMapper.selectBookingPage(new Page<>(1, 100), query, null, craftsmanId, null);
        return decorate(page.getRecords(), true);
    }

    /* ==================================================================================
     *  管理端
     * ================================================================================== */

    @Override
    public void adminCancel(BookingCancelDTO dto) {
        // 管理员不受用户侧取消时限约束（用于场地变更、纠纷处理等场景）
        Booking booking = bookingMapper.selectById(dto.getBookingId());
        BusinessException.throwIf(booking == null, ErrorCode.BOOKING_NOT_FOUND);
        BookingStatusEnum.checkTransfer(booking.getStatus(), BookingStatusEnum.CANCELED);

        doCancel(booking, Constants.CANCEL_BY_ADMIN, dto.getReason());
        Course course = courseMapper.selectById(booking.getCourseId());
        messageService.send(booking.getUserId(), Constants.MSG_BOOKING_AUDIT, "预约已被平台取消",
                "你预约的「" + (course == null ? "课程" : course.getTitle()) + "」已被平台取消"
                        + (StringUtils.hasText(dto.getReason()) ? "，原因：" + dto.getReason() : "")
                        + "。名额已释放，给你带来不便敬请谅解。", booking.getId());
        log.info("[预约] 管理员取消预约 {} 原因={}", booking.getBookingNo(), dto.getReason());
    }

    /* ==================================================================================
     *  取消与归还名额（四条路径共用：用户 / 匠人 / 管理员 / 系统超时）
     * ================================================================================== */

    /**
     * 取消预约：状态流转 + 归还名额，两者在同一事务内，要么都成功要么都回滚。
     *
     * <p><b>为什么归还名额也要加锁：</b>归还走的是 {@code remain_quota = remain_quota + n}，
     * 与提交预约的 {@code remain_quota = remain_quota - n} 是同一行的写操作。
     * 用同一把课程锁把两者串起来，锁的获取顺序与提交路径保持一致
     * （都是「先课程锁、再数据库行锁」），避免出现「一个拿着行锁等课程锁、
     * 另一个拿着课程锁等行锁」的交叉等待。</p>
     *
     * <p><b>幂等由谁保证：</b>由 {@code transferStatus(id, from, to)} 这个带条件的更新保证——
     * 只有把状态从原值改成已取消的那一个线程会返回 1，其余返回 0 直接抛异常，
     * 因此不会重复归还名额（即使重复归还，SQL 里的 {@code LEAST(total_quota, ...)} 也会兜住上限）。</p>
     */
    private void doCancel(Booking booking, int cancelBy, String reason) {
        withCourseLock(booking.getCourseId(), () -> {
            txTemplate.executeWithoutResult(status -> {
                int rows = bookingMapper.transferStatus(booking.getId(),
                        booking.getStatus(), Constants.BOOKING_CANCELED);
                BusinessException.throwIf(rows == 0, ErrorCode.BOOKING_STATUS_ERROR,
                        "预约状态已变化，请刷新后重试");

                    bookingMapper.updateCancelInfo(booking.getId(), cancelBy, reason, LocalDateTime.now());
                courseMapper.restoreQuota(booking.getCourseId(), nvl(booking.getQuota()));
            });
            return null;
        });
        bookingStatusLogService.record(booking.getId(), booking.getStatus(), Constants.BOOKING_CANCELED,
                cancelByText(cancelBy) + (StringUtils.hasText(reason) ? "：" + reason : ""));
        log.info("[预约] 取消成功 {} cancelBy={} 归还名额 {} 个",
                booking.getBookingNo(), cancelBy, nvl(booking.getQuota()));
    }

    /** 审核拒绝：状态流转 + 归还名额 */
    private void doReject(Booking booking, String rejectReason, Long auditorId) {
        withCourseLock(booking.getCourseId(), () -> {
            txTemplate.executeWithoutResult(status -> {
                int rows = bookingMapper.transferStatus(booking.getId(),
                        Constants.BOOKING_WAIT_AUDIT, Constants.BOOKING_REJECTED);
                BusinessException.throwIf(rows == 0, ErrorCode.BOOKING_STATUS_ERROR,
                        "预约状态已变化，请刷新后重试");

                bookingMapper.updateAuditResult(booking.getId(), rejectReason, LocalDateTime.now(), auditorId);
                courseMapper.restoreQuota(booking.getCourseId(), nvl(booking.getQuota()));
            });
            return null;
        });
        // 审核拒绝两条来源共用这里：匠人手点拒绝、系统超时自动拒绝。
        // 轨迹里靠 operator_role 区分（有登录上下文 → 匠人；定时任务 → 系统）
        bookingStatusLogService.record(booking.getId(), Constants.BOOKING_WAIT_AUDIT,
                Constants.BOOKING_REJECTED, rejectReason);
    }

    /** 取消发起方文案（与 booking.cancel_by 同一套编码） */
    private static String cancelByText(int cancelBy) {
        return switch (cancelBy) {
            case Constants.CANCEL_BY_USER -> "用户取消预约";
            case Constants.CANCEL_BY_CRAFTSMAN -> "匠人取消预约";
            case Constants.CANCEL_BY_ADMIN -> "管理员强制取消";
            default -> "系统自动取消";
        };
    }

    /* ==================================================================================
     *  定时任务
     * ================================================================================== */

    @Override
    public int autoRejectTimeout() {
        int timeoutHours = configService.getInt(Constants.CFG_AUDIT_TIMEOUT_HOURS, 24);
        LocalDateTime deadline = LocalDateTime.now().minusHours(timeoutHours);
        List<Booking> list = bookingMapper.selectTimeoutWaitAudit(deadline, TASK_BATCH);
        if (list.isEmpty()) {
            return 0;
        }

        int handled = 0;
        for (Booking booking : list) {
            // 每条单独开事务：一条失败不影响其余，避免「一颗老鼠屎坏一锅汤」
            try {
                Boolean done = txTemplate.execute(status -> {
                    int rows = bookingMapper.transferStatus(booking.getId(),
                            Constants.BOOKING_WAIT_AUDIT, Constants.BOOKING_REJECTED);
                    // 已被匠人处理掉（或上一轮跑过）→ 返回 0，直接跳过，不重复归还名额
                    if (rows == 0) {
                        return Boolean.FALSE;
                    }
                    bookingMapper.updateAuditResult(booking.getId(),
                            "超过 " + timeoutHours + " 小时未审核，系统自动拒绝", LocalDateTime.now(), null);
                    courseMapper.restoreQuota(booking.getCourseId(), nvl(booking.getQuota()));
                    return Boolean.TRUE;
                });
                if (Boolean.TRUE.equals(done)) {
                    handled++;
                    // 系统操作没有登录上下文，轨迹里的操作方会自动落成「系统」，
                    // 与匠人手点拒绝区分得开
                    bookingStatusLogService.record(booking.getId(), Constants.BOOKING_WAIT_AUDIT,
                            Constants.BOOKING_REJECTED,
                            "超过 " + timeoutHours + " 小时未审核，系统自动拒绝");
                    Course course = courseMapper.selectById(booking.getCourseId());
                    messageService.send(booking.getUserId(), Constants.MSG_BOOKING_AUDIT, "预约超时自动取消",
                            "你预约的「" + (course == null ? "课程" : course.getTitle()) + "」因匠人超过 "
                                    + timeoutHours + " 小时未审核，已自动拒绝，名额已释放。", booking.getId());
                }
            } catch (Exception e) {
                log.error("[定时任务] 超时自动拒绝失败 bookingId={} err={}", booking.getId(), e.getMessage());
            }
        }
        if (handled > 0) {
            log.info("[定时任务] 超时未审自动拒绝完成，共处理 {} 条（超过 {} 小时的待审核预约）", handled, timeoutHours);
        }
        return handled;
    }

    @Override
    public int autoFinishCourses() {
        int pushed = 0;

        // ① 先处理「已过结束时间」的课程：状态直接落到已结束
        //    （条件里包含未开始，是为了兜住「定时任务中途才启动、课程早已结束」的情况）
        for (Course course : courseMapper.selectEndDue(TASK_BATCH)) {
            int rows = courseMapper.pushStatus(course.getId(), CourseStatusEnum.FINISHED.getCode());
            if (rows > 0) {
                pushed++;
                // 课程结束 → 已经核销过的预约推进为「已完成」（未核销的保持「已通过」，留作爽约记录）
                // 先取出待推进的 id 再批量更新：批量 UPDATE 只返回行数，拿不到是哪些预约，
                // 而轨迹必须逐条留痕
                List<Long> toFinish = bookingMapper.selectCheckedInIdsByCourse(course.getId());
                int finished = bookingMapper.finishCheckedInByCourse(course.getId());
                for (Long bookingId : toFinish) {
                    bookingStatusLogService.record(bookingId, Constants.BOOKING_CHECKED_IN,
                            Constants.BOOKING_FINISHED, "课程「" + course.getTitle() + "」已结束，自动结课");
                }
                log.info("[定时任务] 课程 {}「{}」已结束，{} 条已核销预约推进为已完成",
                        course.getId(), course.getTitle(), finished);
            }
        }

        // ② 再处理「已到开课时间」的课程：未开始 → 进行中
        //    放在后面处理，避免刚开课又立刻被①判为结束
        for (Course course : courseMapper.selectStartDue(TASK_BATCH)) {
            int rows = courseMapper.pushStatus(course.getId(), CourseStatusEnum.ONGOING.getCode());
            if (rows > 0) {
                pushed++;
                log.info("[定时任务] 课程 {}「{}」已开课，状态改为进行中", course.getId(), course.getTitle());
            }
        }
        return pushed;
    }

    @Override
    public int sendCourseReminders() {
        LocalDateTime deadline = LocalDateTime.now().plusHours(24);
        List<Booking> list = bookingMapper.selectUpcomingPassed(deadline);
        int sent = 0;

        for (Booking booking : list) {
            // 幂等：同一张预约的开课提醒只发一次，避免每一轮扫描都重复轰炸用户
            if (messageService.alreadySent(booking.getUserId(), Constants.MSG_BOOKING_REMIND, booking.getId())) {
                continue;
            }
            Course course = courseMapper.selectById(booking.getCourseId());
            if (course == null) {
                continue;
            }
            messageService.send(booking.getUserId(), Constants.MSG_BOOKING_REMIND, "开课提醒",
                    "你预约的「" + course.getTitle() + "」将于 " + fmt(course.getStartTime())
                            + " 在 " + nvlStr(course.getAddress()) + " 开课，请携带核销码准时到场。",
                    booking.getId());
            sent++;
        }
        if (sent > 0) {
            log.info("[定时任务] 已发送 {} 条开课提醒", sent);
        }
        return sent;
    }

    /* ==================================================================================
     *  内部工具
     * ================================================================================== */

    /**
     * 课程级分布式锁模板（第一层防护的统一入口）
     *
     * <p>提交预约、取消预约、审核拒绝都经由这里拿锁，保证同一门课程的
     * 「扣名额」与「还名额」永远不会交错执行，锁的获取顺序也完全一致。</p>
     */
    private <T> T withCourseLock(Long courseId, Supplier<T> action) {
        String lockKey = Constants.KEY_BOOKING_LOCK + courseId;
        String requestId = UUID.randomUUID().toString();
        if (!redisLockUtil.lock(lockKey, requestId, lockWaitMillis, lockLeaseMillis)) {
            log.warn("[预约] 获取课程锁失败 courseId={}，已有请求持锁且等待超时", courseId);
            throw new BusinessException(ErrorCode.BOOKING_QUEUE_BUSY);
        }
        try {
            return action.get();
        } finally {
            // 无论业务成功或抛异常都释放锁（Lua 比对 requestId，不会误删别人的锁）
            redisLockUtil.unlock(lockKey, requestId);
        }
    }

    /** 取本人预约 */
    private Booking requireMine(Long bookingId) {
        Long userId = UserContext.userId();
        Booking booking = bookingMapper.selectById(bookingId);
        BusinessException.throwIf(booking == null, ErrorCode.BOOKING_NOT_FOUND);
        BusinessException.throwIf(!userId.equals(booking.getUserId()), ErrorCode.BOOKING_NOT_BELONG_TO_YOU);
        return booking;
    }

    /** 匠人端操作：预约必须属于当前登录匠人的课程 */
    private Booking requireCraftsmanBooking(Long bookingId) {
        Long craftsmanId = currentCraftsmanId();
        Booking booking = bookingMapper.selectById(bookingId);
        BusinessException.throwIf(booking == null, ErrorCode.BOOKING_NOT_FOUND);
        BusinessException.throwIf(!craftsmanId.equals(booking.getCraftsmanId()),
                ErrorCode.BOOKING_NOT_BELONG_TO_YOU);
        return booking;
    }

    /** 取当前登录用户名下的匠人 id */
    /**
     * 取当前登录用户名下的匠人 id
     *
     * <p>P8 收口：原先这里自带一份「按 user_id 查匠人」的实现，与
     * {@link CraftsmanHelper} 重复。两份实现意味着两个问题——匠人资质审核状态
     * 只在其中一份里校验（等于没校验），以及规则变化要改两处。
     * 现在统一走 {@code CraftsmanHelper}，审核状态也就跟着全局生效了。</p>
     */
    private Long currentCraftsmanId() {
        return craftsmanHelper.currentCraftsmanId();
    }

    /**
     * 用户能否自行取消；不能时返回原因，能则返回 null。
     *
     * <p>两道限制：课程自己设定的 {@code cancel_deadline}，以及配置项
     * {@code cancel_before_hours}（开课前 N 小时起就不允许自助取消）。</p>
     */
    private String userCancelBlockReason(Course course) {
        LocalDateTime now = LocalDateTime.now();
        if (course.getCancelDeadline() != null && now.isAfter(course.getCancelDeadline())) {
            return "已超过课程设定的取消截止时间 " + fmt(course.getCancelDeadline());
        }
        int hours = configService.getInt(Constants.CFG_CANCEL_BEFORE_HOURS, 24);
        if (hours > 0 && course.getStartTime() != null && now.isAfter(course.getStartTime().minusHours(hours))) {
            return "距开课不足 " + hours + " 小时，如需取消请联系匠人";
        }
        return null;
    }

    /**
     * 给查询出来的预约补上「状态名 + 按钮开关 + 是否已超时」等派生字段
     *
     * <p>按钮开关统一由后端算好下发，前端不重写一遍状态规则，避免两边判断不一致。
     * 「是否已评价」用一次批量查询 + Set 判断，不做逐条回查（N+1）。</p>
     */
    private List<BookingVO> decorate(List<BookingVO> list, boolean craftsmanSide) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Set<Long> reviewed = new HashSet<>();
        List<Long> ids = new ArrayList<>();
        for (BookingVO vo : list) {
            if (vo.getId() != null) {
                ids.add(vo.getId());
            }
        }
        if (!ids.isEmpty()) {
            List<Long> reviewedIds = reviewMapper.selectReviewedBookingIds(ids);
            if (reviewedIds != null) {
                reviewed.addAll(reviewedIds);
            }
        }

        int timeoutHours = configService.getInt(Constants.CFG_AUDIT_TIMEOUT_HOURS, 24);
        LocalDateTime now = LocalDateTime.now();

        for (BookingVO vo : list) {
            int status = vo.getStatus() == null ? 0 : vo.getStatus();
            vo.setStatusDesc(BookingStatusEnum.of(status).getDesc());
            vo.setCancelByDesc(cancelByDesc(vo.getCancelBy()));

            boolean checkedIn = status == Constants.BOOKING_CHECKED_IN;
            boolean timeout = status == Constants.BOOKING_WAIT_AUDIT
                    && vo.getCreateTime() != null
                    && vo.getCreateTime().isBefore(now.minusHours(timeoutHours));
            vo.setTimeout(timeout);

            if (craftsmanSide) {
                vo.setCanAudit(status == Constants.BOOKING_WAIT_AUDIT);
                vo.setCanCheckin(status == Constants.BOOKING_PASSED);
            } else {
                String block = null;
                if (status == Constants.BOOKING_WAIT_AUDIT || status == Constants.BOOKING_PASSED) {
                    Course course = new Course();
                    course.setCancelDeadline(vo.getCourseCancelDeadline());
                    course.setStartTime(vo.getCourseStartTime());
                    block = userCancelBlockReason(course);
                }
                vo.setCanCancel((status == Constants.BOOKING_WAIT_AUDIT || status == Constants.BOOKING_PASSED)
                        && block == null);
                vo.setCancelDisabledReason(block);
                vo.setCanShowCheckinCode(status == Constants.BOOKING_PASSED || checkedIn);
                vo.setCanViewCourse(true);
                // 课程结束、预约已完成、且还没评价过 → 可以评价
                vo.setCanReview(status == Constants.BOOKING_FINISHED && !reviewed.contains(vo.getId()));
            }
        }
        return list;
    }

    private String cancelByDesc(Integer cancelBy) {
        if (cancelBy == null) {
            return null;
        }
        return switch (cancelBy) {
            case Constants.CANCEL_BY_USER -> "用户取消";
            case Constants.CANCEL_BY_CRAFTSMAN -> "匠人取消";
            case Constants.CANCEL_BY_ADMIN -> "管理员取消";
            case Constants.CANCEL_BY_SYSTEM -> "系统超时";
            default -> "其他";
        };
    }

    /** 把 group by 结果转成 {状态: 数量} 并补一个 all 总计 */
    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        long all = 0L;
        for (Map<String, Object> row : rows) {
            int status = ((Number) row.get("status")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            map.put(String.valueOf(status), cnt);
            all += cnt;
        }
        map.put("all", all);
        return map;
    }

    /** 生成预约单号：BK + 时间(14) + 用户后四位 + 6 位随机（长度 26，符合 32 位上限） */
    private String generateBookingNo(Long userId) {
        String random = String.format("%06d", (int) (Math.random() * 1_000_000));
        return "BK" + NO_FMT.format(LocalDateTime.now())
                + String.format("%04d", userId % 10000) + random;
    }

    /**
     * 生成核销码：UUID 去掉横杠取前 8 位并大写。
     *
     * <p>表上有唯一索引 {@code uk_checkin_code} 兜底；8 位十六进制有约 43 亿种组合，
     * 教学规模的数据量下碰撞概率可忽略。</p>
     */
    private String generateCheckinCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static String fmt(LocalDateTime time) {
        return time == null ? "待定" : TIME_FMT.format(time);
    }

    private static String nvlStr(String v) {
        return v == null ? "" : v;
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
