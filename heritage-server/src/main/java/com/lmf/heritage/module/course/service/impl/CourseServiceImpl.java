package com.lmf.heritage.module.course.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.booking.mapper.BookingMapper;
import com.lmf.heritage.module.booking.service.BookingConfigService;
import com.lmf.heritage.module.course.dto.CourseQueryDTO;
import com.lmf.heritage.module.course.dto.CourseSaveDTO;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.enums.CourseStatusEnum;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.course.service.CourseService;
import com.lmf.heritage.module.course.vo.CourseCalendarVO;
import com.lmf.heritage.module.course.vo.CourseDetailVO;
import com.lmf.heritage.module.course.vo.CourseListVO;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.mapper.CraftsmanMapper;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.util.CacheHelper;
import com.lmf.heritage.util.CraftsmanHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 课程服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final CraftsmanMapper craftsmanMapper;
    private final BookingMapper bookingMapper;
    private final GoodsMapper goodsMapper;
    private final BookingConfigService configService;
    private final CacheHelper cacheHelper;
    private final CraftsmanHelper craftsmanHelper;

    /** 日历日期文案格式：「9月20日」 */
    private static final DateTimeFormatter DATE_MD = DateTimeFormatter.ofPattern("M月d日");
    /** 星期文案（索引 0 对应周一，与 DayOfWeek.getValue() - 1 对齐） */
    private static final String[] WEEK_CN = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    @Override
    public PageResult<CourseListVO> pageList(CourseQueryDTO query) {
        query.normalize();
        IPage<CourseListVO> page = courseMapper.selectCoursePage(
                new Page<>(query.getPageNum(), query.getPageSize()), query);
        return PageResult.of(page, decorate(page.getRecords()));
    }

    @Override
    public CourseDetailVO detail(Long id) {
        Course course = getById(id);
        // 未通过平台审核的课程不对外可见（草稿只有匠人自己能在「我的课程」里看到）
        BusinessException.throwIf(course == null || !Integer.valueOf(1).equals(course.getAuditStatus()),
                ErrorCode.COURSE_NOT_FOUND);

        CourseListVO base = courseMapper.selectCourseVoById(id);
        BusinessException.throwIf(base == null, ErrorCode.COURSE_NOT_FOUND);

        CourseDetailVO vo = new CourseDetailVO();
        BeanUtils.copyProperties(base, vo);
        decorate(List.of(vo));

        vo.setDescription(course.getDescription());
        vo.setDetail(course.getDetail());
        vo.setDurationText(durationText(course.getDuration()));

        // 浏览量：只累加 Redis 计数器，不碰数据库；返回时把实时增量叠加到库里的基数上
        long delta = cacheHelper.increment(Constants.KEY_COURSE_VIEW + id);
        vo.setViewCount(nvl(base.getViewCount()) + (int) delta);

        Craftsman craftsman = craftsmanMapper.selectById(course.getCraftsmanId());
        CraftsmanSimpleVO craftsmanVO = CraftsmanSimpleVO.from(craftsman);
        if (craftsmanVO != null) {
            // 匠人卡片上的「在售作品数」，复用商品模块已有的统计
            craftsmanVO.setGoodsCount((int) goodsMapper.countByCraftsman(craftsman.getId()));
        }
        vo.setCraftsman(craftsmanVO);

        Map<String, Object> stat = courseMapper.reviewStat(id);
        if (stat != null) {
            vo.setReviewCount(stat.get("cnt") == null ? 0L : ((Number) stat.get("cnt")).longValue());
            vo.setAvgScore(stat.get("avgScore") == null ? 0D : ((Number) stat.get("avgScore")).doubleValue());
        }
        return vo;
    }

    @Override
    public List<CourseListVO> listByCraftsman(Long craftsmanId, int limit) {
        List<CourseListVO> list = courseMapper.listByCraftsman(craftsmanId, limit <= 0 ? 6 : limit);
        return decorate(list);
    }

    @Override
    public PageResult<CourseListVO> myCourses(CourseQueryDTO query) {
        query.normalize();
        Long craftsmanId = currentCraftsmanId();
        IPage<CourseListVO> page = courseMapper.selectMyCoursePage(
                new Page<>(query.getPageNum(), query.getPageSize()), craftsmanId, query);
        return PageResult.of(page, decorate(page.getRecords()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCourse(CourseSaveDTO dto) {
        Long craftsmanId = currentCraftsmanId();
        BusinessException.throwIf(!dto.getEndTime().isAfter(dto.getStartTime()),
                ErrorCode.PARAM_ERROR, "结束时间必须晚于开课时间");

        LocalDateTime bookingDeadline = dto.getBookingDeadline() != null
                ? dto.getBookingDeadline() : dto.getStartTime();
        LocalDateTime cancelDeadline = dto.getCancelDeadline() != null
                ? dto.getCancelDeadline() : dto.getStartTime();
        BusinessException.throwIf(bookingDeadline.isAfter(dto.getStartTime()),
                ErrorCode.PARAM_ERROR, "预约截止时间不能晚于开课时间");
        BusinessException.throwIf(cancelDeadline.isAfter(dto.getStartTime()),
                ErrorCode.PARAM_ERROR, "可取消截止时间不能晚于开课时间");

        if (dto.getId() == null) {
            Course course = new Course();
            applyFields(course, dto);
            course.setCraftsmanId(craftsmanId);
            // 新发布的课程剩余名额 = 总名额
            course.setRemainQuota(dto.getTotalQuota());
            course.setBookingDeadline(bookingDeadline);
            course.setCancelDeadline(cancelDeadline);
            // 课程状态由定时任务按时间推进，这里新建一律是「未开始」
            course.setStatus(CourseStatusEnum.NOT_STARTED.getCode());
            // 匠人自主发布、暂免平台审核（管理端课程审核在 P8 接入，先让前台能看到）
            course.setAuditStatus(1);
            course.setViewCount(0);
            course.setBookingCount(0);
            save(course);
            log.info("[课程] 匠人 {} 新建课程 {}「{}」总名额 {}", craftsmanId, course.getId(),
                    course.getTitle(), course.getTotalQuota());
            return course.getId();
        }

        Course exist = getById(dto.getId());
        BusinessException.throwIf(exist == null, ErrorCode.COURSE_NOT_FOUND);
        BusinessException.throwIf(!craftsmanId.equals(exist.getCraftsmanId()),
                ErrorCode.NO_PERMISSION, "只能编辑自己开设的课程");

        // 改总名额时要同步剩余名额：已约出去的部分不能被「改小总名额」吞掉
        int booked = nvl(exist.getTotalQuota()) - nvl(exist.getRemainQuota());
        int newRemain = dto.getTotalQuota() - booked;
        BusinessException.throwIf(newRemain < 0, ErrorCode.PARAM_ERROR,
                "总名额不能小于已预约的 " + booked + " 个");

        applyFields(exist, dto);
        exist.setRemainQuota(newRemain);
        exist.setBookingDeadline(bookingDeadline);
        exist.setCancelDeadline(cancelDeadline);
        updateById(exist);

        // 课程信息变了，首页/列表缓存必须失效，否则前台还看到旧的名额与时间
        evictCache();
        log.info("[课程] 匠人 {} 编辑课程 {}「{}」总名额 {} 剩余名额 {}",
                craftsmanId, exist.getId(), exist.getTitle(), dto.getTotalQuota(), newRemain);
        return exist.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCourse(Long id) {
        Long craftsmanId = currentCraftsmanId();
        Course course = getById(id);
        BusinessException.throwIf(course == null, ErrorCode.COURSE_NOT_FOUND);
        BusinessException.throwIf(!craftsmanId.equals(course.getCraftsmanId()),
                ErrorCode.NO_PERMISSION, "只能删除自己开设的课程");

        // 还有待审核 / 已通过的预约时不能删：否则用户手里会留下指向不存在课程的预约
        int active = bookingMapper.countActiveByCourse(id);
        BusinessException.throwIf(active > 0, ErrorCode.PARAM_ERROR,
                "该课程还有 " + active + " 条未结束的预约，请先处理后再删除");

        removeById(id);
        evictCache();
        log.info("[课程] 匠人 {} 删除课程 {}「{}」", craftsmanId, id, course.getTitle());
    }

    /* ==================== 匠人端课程运营（P7） ==================== */

    @Override
    public List<CourseCalendarVO> calendar(LocalDate start, LocalDate end) {
        BusinessException.throwIf(start == null || end == null,
                ErrorCode.PARAM_ERROR, "请指定排期查询的起止日期");
        BusinessException.throwIf(end.isBefore(start),
                ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        // 日历视图没有跨年拉取的需求；设上限也是为了避免一次把全部课程捞回来
        BusinessException.throwIf(ChronoUnit.DAYS.between(start, end) > 366,
                ErrorCode.PARAM_ERROR, "排期查询区间最长 366 天");

        Long craftsmanId = craftsmanHelper.currentCraftsmanId();
        // 左闭右开：把 end 加一天再取下界，正好覆盖 end 当天 23:59 开课的课
        List<CourseListVO> list = courseMapper.selectCalendarList(craftsmanId,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        decorate(list);

        // TreeMap 保证日期天然升序，前端不必再排一次
        Map<LocalDate, List<CourseListVO>> grouped = list.stream()
                .filter(v -> v.getStartTime() != null)
                .collect(Collectors.groupingBy(v -> v.getStartTime().toLocalDate(),
                        TreeMap::new, Collectors.toList()));

        List<CourseCalendarVO> result = new ArrayList<>(grouped.size());
        grouped.forEach((date, courses) -> result.add(buildDay(date, courses)));
        log.debug("[课程] 匠人 {} 查询排期 {} ~ {}，命中 {} 天 {} 门课",
                craftsmanId, start, end, result.size(), list.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setBookingOpen(Long courseId, boolean open) {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();
        Course course = getById(courseId);
        BusinessException.throwIf(course == null, ErrorCode.COURSE_NOT_FOUND);
        BusinessException.throwIf(!craftsmanId.equals(course.getCraftsmanId()),
                ErrorCode.NO_PERMISSION, "只能操作自己开设的课程");

        // 已开课 / 已结束的课程谈不上「开放预约」——状态是事实，不由开关决定
        BusinessException.throwIf(
                !Integer.valueOf(CourseStatusEnum.NOT_STARTED.getCode()).equals(course.getStatus()),
                ErrorCode.COURSE_STATUS_ERROR, "课程已开课或已结束，无法调整预约开关");

        Course update = new Course();
        update.setId(courseId);
        if (open) {
            BusinessException.throwIf(course.getStartTime() == null,
                    ErrorCode.PARAM_ERROR, "课程未设置开课时间，无法开放预约");
            // 重新开放 = 把预约窗口拉到最大：截止时间顶到开课时间
            update.setBookingDeadline(course.getStartTime());
        } else {
            // 关闭 = 截止时间落到此刻。整条「是否还能预约」的判定链
            // （status / remainQuota / bookingDeadline / advanceHours）里被这一步关掉的那一环，
            // 不会留下任何别的后门
            update.setBookingDeadline(LocalDateTime.now());
        }
        updateById(update);
        evictCache();
        log.info("[课程] 匠人 {} {}课程 {}「{}」的预约",
                craftsmanId, open ? "重新开放" : "关闭", courseId, course.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishCourse(Long courseId) {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();
        Course course = getById(courseId);
        BusinessException.throwIf(course == null, ErrorCode.COURSE_NOT_FOUND);
        BusinessException.throwIf(!craftsmanId.equals(course.getCraftsmanId()),
                ErrorCode.NO_PERMISSION, "只能操作自己开设的课程");

        // pushStatus 自带 status <> 目标值 的条件，重复调用返回 0 行，
        // 正好用来区分「确实结束了一门课」与「本来就已结束」
        int rows = courseMapper.pushStatus(courseId, CourseStatusEnum.FINISHED.getCode());
        BusinessException.throwIf(rows == 0, ErrorCode.COURSE_ALREADY_ENDED);

        evictCache();
        log.info("[课程] 匠人 {} 提前结束课程 {}「{}」", craftsmanId, courseId, course.getTitle());
    }

    /* ==================== 内部方法 ==================== */

    /** 组装日历中的一天：课程数 + 名额使用情况 */
    private CourseCalendarVO buildDay(LocalDate date, List<CourseListVO> courses) {
        int total = 0;
        int booked = 0;
        for (CourseListVO c : courses) {
            total += nvl(c.getTotalQuota());
            booked += nvl(c.getBookedQuota());
        }
        CourseCalendarVO day = new CourseCalendarVO();
        day.setDate(date);
        day.setDateText(dateText(date));
        day.setCourseCount(courses.size());
        day.setTotalQuota(total);
        day.setBookedQuota(booked);
        // 满座率：总名额为 0 时约定为 0，避免除零
        day.setUsedRate(total <= 0 ? 0 : (int) Math.round(booked * 100.0 / total));
        day.setCourses(courses);
        return day;
    }

    /** 「9月20日 周日」 */
    private static String dateText(LocalDate date) {
        return DATE_MD.format(date) + " " + WEEK_CN[date.getDayOfWeek().getValue() - 1];
    }

    /** 把 DTO 的可直接映射字段搬到实体上 */
    private void applyFields(Course course, CourseSaveDTO dto) {
        course.setCategoryId(dto.getCategoryId());
        course.setTitle(dto.getTitle());
        course.setSubtitle(dto.getSubtitle());
        course.setCover(dto.getCover());
        course.setDescription(dto.getDescription());
        course.setDetail(dto.getDetail());
        course.setAddress(dto.getAddress());
        course.setStartTime(dto.getStartTime());
        course.setEndTime(dto.getEndTime());
        course.setPrice(dto.getPrice() == null ? java.math.BigDecimal.ZERO : dto.getPrice());
        course.setTotalQuota(dto.getTotalQuota());
        course.setDuration(dto.getDuration());
        course.setDifficulty(dto.getDifficulty() == null ? 1 : dto.getDifficulty());
    }

    /**
     * 给课程补上「状态名、倒计时、能不能预约、以及不能约的原因」
     *
     * <p>「能不能预约」这件事涉及课程状态、剩余名额、预约截止时间与配置里的最少提前预约时长
     * 四个条件，前端自己判断极易漏项、且和後端规则漂移。统一在这里算好后，
     * 前端只负责把 {@code canBook} 渲染成按钮的可用状态即可。</p>
     */
    private List<CourseListVO> decorate(List<CourseListVO> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        int advanceHours = configService.getInt(Constants.CFG_BOOKING_ADVANCE_HOURS, 2);
        LocalDateTime now = LocalDateTime.now();

        for (CourseListVO vo : list) {
            vo.setStatusDesc(CourseStatusEnum.descOf(vo.getStatus()));

            // 倒计时：以预约截止时间为准（没设则回落到开课时间）
            LocalDateTime deadline = vo.getBookingDeadline() != null ? vo.getBookingDeadline() : vo.getStartTime();
            long remainSeconds = deadline == null ? 0L : Duration.between(now, deadline).getSeconds();
            vo.setRemainSeconds(remainSeconds);

            String reason = null;
            if (Integer.valueOf(CourseStatusEnum.FINISHED.getCode()).equals(vo.getStatus())) {
                reason = "课程已结束";
            } else if (Integer.valueOf(CourseStatusEnum.ONGOING.getCode()).equals(vo.getStatus())) {
                reason = "课程已开课";
            } else if (nvl(vo.getRemainQuota()) <= 0) {
                reason = "名额已约满";
            } else if (remainSeconds <= 0) {
                reason = "已过预约截止时间";
            } else if (advanceHours > 0 && vo.getStartTime() != null
                    && now.isAfter(vo.getStartTime().minusHours(advanceHours))) {
                reason = "距开课不足 " + advanceHours + " 小时";
            }
            vo.setCanBook(reason == null);
            vo.setClosedReason(reason);
        }
        return list;
    }

    /** 课时分钟数转成可读文本 */
    private String durationText(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return null;
        }
        if (minutes < 60) {
            return minutes + " 分钟";
        }
        double hours = minutes / 60.0;
        if (hours == Math.floor(hours)) {
            return (int) hours + " 小时";
        }
        return String.format("%.1f 小时", hours);
    }

    /** 取当前登录用户名下的匠人 id（统一走 CraftsmanHelper，规则只维护一处） */
    private Long currentCraftsmanId() {
        return craftsmanHelper.currentCraftsmanId();
    }

    /** 课程变更后清掉相关缓存 */
    private void evictCache() {
        cacheHelper.deleteByPrefix(Constants.KEY_COURSE_LIST);
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
