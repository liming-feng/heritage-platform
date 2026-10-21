package com.lmf.heritage.module.course.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.course.dto.AdminCourseQueryDTO;
import com.lmf.heritage.module.course.dto.CourseAuditDTO;
import com.lmf.heritage.module.course.dto.CourseOffShelfDTO;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.enums.CourseStatusEnum;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.course.service.AdminCourseService;
import com.lmf.heritage.module.course.vo.AdminCourseDetailVO;
import com.lmf.heritage.module.course.vo.AdminCourseVO;
import com.lmf.heritage.module.course.vo.CourseExcelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端 · 课程审核实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCourseServiceImpl implements AdminCourseService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int EXPORT_LIMIT = 5000;

    /** 与商品违规下架同一个约定：让驳回原因自述来历 */
    private static final String OFF_SHELF_TAG = "【违规下架】";

    private final CourseMapper courseMapper;

    /* ==================== 列表与详情 ==================== */

    @Override
    public PageResult<AdminCourseVO> page(AdminCourseQueryDTO query) {
        query.normalize();
        IPage<AdminCourseVO> result = queryPage(query, query.getPageNum(), query.getPageSize());
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 查询 + 补齐展示字段（分页与导出共用这一个入口）
     *
     * <p>状态与审核文案由 {@link #decorate} 在 Java 端回填，不在 SQL 里算。导出若另走
     * 一条「只查不补」的路，Excel 里就会出现整列空白——而文件打得开、行数也对，光看
     * 导出结果很难发现。绑成一个方法，两条路就没有分叉的可能。</p>
     */
    private IPage<AdminCourseVO> queryPage(AdminCourseQueryDTO query, Long pageNum, Long pageSize) {
        Page<AdminCourseVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminCourseVO> result = courseMapper.selectAdminCoursePage(page, query);
        result.getRecords().forEach(this::decorate);
        return result;
    }

    @Override
    public AdminCourseDetailVO auditDetail(Long id) {
        AdminCourseDetailVO vo = courseMapper.selectAdminCourseDetail(id);
        BusinessException.throwIf(vo == null, ErrorCode.COURSE_NOT_FOUND);

        vo.setStatusDesc(CourseStatusEnum.descOf(vo.getStatus()));
        vo.setAuditStatusDesc(auditText(vo.getAuditStatus()));
        vo.setUsedRate(rate(vo.getBookedQuota(), vo.getTotalQuota()));

        int checkedIn = nz(vo.getBookingCheckedIn());
        int passed = nz(vo.getBookingPassed());
        vo.setAttendPeople(checkedIn + nz(vo.getBookingFinished()));
        vo.setAbsentPeople(passed);
        // 核销率只在已结束的课程上有意义：课还没开，说「应到未到」是在冤枉人。
        // 无已结束课程时返回 null 而不是 0% —— 前端据此显示「—」而不是一个误导的零
        vo.setCheckinRate(
                Integer.valueOf(Constants.COURSE_ENDED).equals(vo.getStatus())
                        ? rate(vo.getAttendPeople(), vo.getAttendPeople() + passed)
                        : null);
        return vo;
    }

    /* ==================== 审核 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(CourseAuditDTO dto) {
        List<Long> ids = distinctIds(dto.getIds());
        boolean pass = Boolean.TRUE.equals(dto.getPass());
        String reason = trim(dto.getReason());
        BusinessException.throwIf(!pass && reason == null, ErrorCode.AUDIT_REASON_REQUIRED,
                "驳回时必须说明原因，匠人要照着改");

        List<Course> courses = courseMapper.selectBatchIds(ids);
        BusinessException.throwIf(courses.size() != ids.size(), ErrorCode.COURSE_NOT_FOUND,
                "所选课程中有 " + (ids.size() - courses.size()) + " 门不存在或已被删除");

        // 整批回滚：只要有任意一门不是待审核状态就整批拒绝。
        // 单门审核也走这条路径（ids 只有一个元素），不给「单件」留一份只差循环的实现
        List<String> bad = courses.stream()
                .filter(c -> !Integer.valueOf(Constants.AUDIT_WAIT).equals(c.getAuditStatus()))
                .map(c -> c.getTitle() + "（" + auditText(c.getAuditStatus()) + "）")
                .toList();
        BusinessException.throwIf(!bad.isEmpty(), ErrorCode.COURSE_AUDIT_STATUS_ERROR,
                "以下课程不是待审核状态，请刷新后重试：" + String.join("、", bad));

        int affected = courseMapper.updateAuditByIds(ids, Constants.AUDIT_WAIT,
                pass ? Constants.AUDIT_PASSED : Constants.AUDIT_REJECTED,
                pass ? "" : reason);
        BusinessException.throwIf(affected != ids.size(), ErrorCode.COURSE_AUDIT_STATUS_ERROR,
                "实际生效 " + affected + " 门，与提交的 " + ids.size() + " 门不一致，已回滚");
        log.info("[课程审核] {} 门 {}", affected, pass ? "通过" : "驳回：" + reason);
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int offShelf(CourseOffShelfDTO dto) {
        List<Long> ids = distinctIds(dto.getIds());
        String reason = trim(dto.getReason());
        BusinessException.throwIf(reason == null, ErrorCode.AUDIT_REASON_REQUIRED, "下架原因必填");

        List<Course> courses = courseMapper.selectBatchIds(ids);
        BusinessException.throwIf(courses.size() != ids.size(), ErrorCode.COURSE_NOT_FOUND,
                "所选课程中有 " + (ids.size() - courses.size()) + " 门不存在或已被删除");

        List<String> bad = courses.stream()
                .filter(c -> !Integer.valueOf(Constants.AUDIT_PASSED).equals(c.getAuditStatus()))
                .map(c -> c.getTitle() + "（" + auditText(c.getAuditStatus()) + "）")
                .toList();
        BusinessException.throwIf(!bad.isEmpty(), ErrorCode.COURSE_AUDIT_STATUS_ERROR,
                "以下课程不在「已通过」状态，无法违规下架：" + String.join("、", bad));

        int affected = courseMapper.updateAuditByIds(ids, Constants.AUDIT_PASSED,
                Constants.AUDIT_REJECTED, OFF_SHELF_TAG + reason);
        BusinessException.throwIf(affected != ids.size(), ErrorCode.COURSE_AUDIT_STATUS_ERROR,
                "实际生效 " + affected + " 门，与提交的 " + ids.size() + " 门不一致，已回滚");

        // 已有的预约不动，只提醒一句：下架不等于取消预约
        long booked = courses.stream().mapToLong(c -> c.getTotalQuota() == null || c.getRemainQuota() == null
                ? 0 : c.getTotalQuota() - c.getRemainQuota()).sum();
        log.warn("[课程违规下架] {} 门，涉及已约名额 {} 个（预约记录保留，需另行处理退款/通知），原因：{}",
                affected, booked, reason);
        return affected;
    }

    /* ==================== 导出 ==================== */

    @Override
    public List<CourseExcelVO> exportList(AdminCourseQueryDTO query) {
        query.normalize();
        // 与分页共用 queryPage：文案字段一并补齐，导出的就是页面上看到的那一份
        List<AdminCourseVO> records = queryPage(query, 1L, (long) EXPORT_LIMIT).getRecords();
        return records.stream().map(v -> {
            CourseExcelVO e = new CourseExcelVO();
            e.setId(v.getId());
            e.setTitle(v.getTitle());
            e.setCategoryName(v.getCategoryName());
            e.setCraftsmanName(v.getCraftsmanName());
            e.setCraftType(v.getCraftType());
            e.setPrice(v.getPrice());
            e.setAddress(v.getAddress());
            e.setStartTime(v.getStartTime());
            e.setEndTime(v.getEndTime());
            e.setTotalQuota(v.getTotalQuota());
            e.setRemainQuota(v.getRemainQuota());
            e.setBookedQuota(v.getBookedQuota());
            e.setStatusDesc(v.getStatusDesc());
            e.setAuditStatusDesc(v.getAuditStatusDesc());
            e.setRejectReason(v.getRejectReason());
            e.setCreateTimeText(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== 内部工具 ==================== */

    private void decorate(AdminCourseVO vo) {
        vo.setStatusDesc(CourseStatusEnum.descOf(vo.getStatus()));
        vo.setAuditStatusDesc(auditText(vo.getAuditStatus()));
        vo.setUsedRate(rate(vo.getBookedQuota(), vo.getTotalQuota()));
    }

    /** 课程审核文案：与商品共用同一套 「0待审核 / 1已通过 / 2已驳回」三态 */
    static String auditText(Integer s) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case Constants.AUDIT_WAIT -> "待审核";
            case Constants.AUDIT_PASSED -> "已通过";
            case Constants.AUDIT_REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    /**
     * 百分数（保留 1 位小数）
     *
     * <p>分母为 0 或 null 时返回 {@code null} 而不是 0：一门总名额为 0 的课程
     * 「使用率 0%」听起来像「没人报名」，实际是「没有名额可失」。</p>
     */
    private static Double rate(Integer numerator, Integer denominator) {
        int d = nz(denominator);
        if (d <= 0) {
            return null;
        }
        return BigDecimal.valueOf(nz(numerator) * 100.0 / d)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static List<Long> distinctIds(List<Long> ids) {
        List<Long> list = ids.stream().filter(Objects::nonNull).distinct().toList();
        BusinessException.throwIf(list.isEmpty(), ErrorCode.ADMIN_OPERATION_EMPTY);
        return list;
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
