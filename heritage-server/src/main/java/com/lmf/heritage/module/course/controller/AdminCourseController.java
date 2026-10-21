package com.lmf.heritage.module.course.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.course.dto.AdminCourseQueryDTO;
import com.lmf.heritage.module.course.dto.CourseAuditDTO;
import com.lmf.heritage.module.course.dto.CourseOffShelfDTO;
import com.lmf.heritage.module.course.service.AdminCourseService;
import com.lmf.heritage.module.course.vo.AdminCourseDetailVO;
import com.lmf.heritage.module.course.vo.AdminCourseVO;
import com.lmf.heritage.module.course.vo.CourseExcelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端 · 课程审核
 *
 * <p><b>关于「批量上下架」</b>：课程没有独立的上下架开关，前台可见性由
 * {@code audit_status = 1} 唯一决定。因此「上架」= 批量审核通过，
 * 「下架」= 批量违规下架，两者都是本控制器的接口 —— 不额外造一个
 * 与审核语义重叠的 shelf 开关，否则同一个事实会有两个来源。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-课程审核", description = "全平台课程列表 / 审核详情 / 通过驳回 / 批量审核 / 违规下架 / 导出")
@RestController
@RequestMapping("/admin/course")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    @Operation(summary = "全平台课程列表",
            description = "status 0未开始/1进行中/2已结束，auditStatus 0待审核/1已通过/2已驳回，"
                    + "categoryId 分类，craftsmanId 按匠人下钻，keyword 匹配标题/副标题/匠人姓名，"
                    + "startTime~endTime 开课时间范围；sort new(默认)/start/hot/quota")
    @GetMapping("/list")
    public Result<PageResult<AdminCourseVO>> list(AdminCourseQueryDTO query) {
        return Result.success(adminCourseService.page(query));
    }

    @Operation(summary = "课程审核详情",
            description = "含课程简介与详情富文本、匠人联系方式、报名结构（待审/通过/拒绝/取消/已核销/已结课）"
                    + "与核销率。核销率只在课程已结束时返回，未结束为 null")
    @GetMapping("/{id}/audit-detail")
    public Result<AdminCourseDetailVO> auditDetail(@PathVariable("id") Long id) {
        return Result.success(adminCourseService.auditDetail(id));
    }

    @Operation(summary = "课程审核（单件 / 批量共用）",
            description = "单次最多 50 门，ids 只放一个即为单件。仅「待审核」的课程可审，否则返回 8004。"
                    + "通过后前台立即可见并可预约；驳回时 reason 必填（8006）。整批回滚语义")
    @OperationLog("课程审核")
    @PostMapping("/audit")
    public Result<Integer> audit(@RequestBody @Valid CourseAuditDTO dto) {
        return Result.success(adminCourseService.audit(dto));
    }

    @Operation(summary = "违规下架（批量）",
            description = "单次最多 50 门，reason 必填。起始状态必须是「已通过」，否则返回 8004。"
                    + "下架会把审核状态置为「已驳回」并写成「【违规下架】原因」，前台立即不可见。"
                    + "已有预约记录保留不回溯取消，如需取消请另行调用管理员强制取消预约")
    @OperationLog("课程违规下架")
    @PostMapping("/off-shelf")
    public Result<Integer> offShelf(@RequestBody @Valid CourseOffShelfDTO dto) {
        return Result.success(adminCourseService.offShelf(dto));
    }

    @Operation(summary = "导出课程 Excel",
            description = "筛选条件与列表一致，最多 5000 条。价格与名额为数值型，可直接统计")
    @GetMapping("/export")
    public void export(AdminCourseQueryDTO query, HttpServletResponse response) throws IOException {
        List<CourseExcelVO> rows = adminCourseService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("课程列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), CourseExcelVO.class)
                .sheet("课程列表")
                .doWrite(rows);
        log.info("[课程审核] 导出 {} 条", rows.size());
    }
}
