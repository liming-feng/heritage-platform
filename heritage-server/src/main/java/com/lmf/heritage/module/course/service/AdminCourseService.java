package com.lmf.heritage.module.course.service;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.course.dto.AdminCourseQueryDTO;
import com.lmf.heritage.module.course.dto.CourseAuditDTO;
import com.lmf.heritage.module.course.dto.CourseOffShelfDTO;
import com.lmf.heritage.module.course.vo.AdminCourseDetailVO;
import com.lmf.heritage.module.course.vo.AdminCourseVO;
import com.lmf.heritage.module.course.vo.CourseExcelVO;

import java.util.List;

/**
 * 管理端 · 课程审核
 *
 * <p>课程审核与商品审核有一处关键差别：课程的「上架」不是一个独立开关，
 * 前台是否可见完全由 {@code audit_status = 1} 决定（{@code selectCoursePage} 的固定条件）。
 * 所以这里只有两个动作：<b>审核</b>（通过 / 驳回，通过即前台可见）
 * 与<b>违规下架</b>（audit_status 打回，前台立即不可见）。</p>
 *
 * @author 李明峰 23100025
 */
public interface AdminCourseService {

    /** 全平台课程列表 */
    PageResult<AdminCourseVO> page(AdminCourseQueryDTO query);

    /** 课程审核详情（含报名结构与核销率） */
    AdminCourseDetailVO auditDetail(Long id);

    /** 审核：通过 / 驳回（单件与批量同一条路径，ids 数量决定批量与否） */
    int audit(CourseAuditDTO dto);

    /** 违规下架：起始状态必须是「已通过」 */
    int offShelf(CourseOffShelfDTO dto);

    /** 导出用：与列表同一套筛选条件 */
    List<CourseExcelVO> exportList(AdminCourseQueryDTO query);
}
