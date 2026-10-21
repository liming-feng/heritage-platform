package com.lmf.heritage.module.booking.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.booking.dto.AdminBookingQueryDTO;
import com.lmf.heritage.module.booking.dto.BookingCancelDTO;
import com.lmf.heritage.module.booking.service.AdminBookingService;
import com.lmf.heritage.module.booking.service.BookingService;
import com.lmf.heritage.module.booking.vo.AdminBookingDetailVO;
import com.lmf.heritage.module.booking.vo.AdminBookingExcelVO;
import com.lmf.heritage.module.booking.vo.BookingStatusLogVO;
import com.lmf.heritage.module.booking.vo.BookingVO;
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
 * 管理端 · 预约监管（题目的核心业务的监管视角）
 *
 * <p>这里有两类接口：<br>
 * ① <b>只读监管</b>（列表、详情、轨迹、导出）—— 委托给 {@code AdminBookingService}；<br>
 * ② <b>会改数据的动作</b>（强制取消、定时任务手动触发）—— 委托给 {@code BookingService}，
 * 因为它们必须走与用户取消、匠人取消完全相同的那条「归还名额」路径，
 * 另写一份实现迟早会在名额上对不上账。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-预约监管", description = "全平台预约列表 / 详情与流转轨迹 / 强制取消 / 导出 / 定时任务手动触发")
@RestController
@RequestMapping("/admin/booking")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminBookingController {

    private final BookingService bookingService;
    private final AdminBookingService adminBookingService;

    @Operation(summary = "全平台预约列表",
            description = "status 0待审核/1已通过/2已拒绝/3已取消/4已核销/5已完成，courseId 课程，"
                    + "userId 预约人，craftsmanId 匠人，keyword 匹配预约单号/课程标题/联系人/联系电话/匠人姓名，"
                    + "startTime~endTime 提交时间范围，onlyTimeout 只看超时未审；sort new(默认)/old/course")
    @GetMapping("/list")
    public Result<PageResult<BookingVO>> list(AdminBookingQueryDTO query) {
        return Result.success(adminBookingService.page(query));
    }

    @Operation(summary = "预约详情",
            description = "预约主体信息 + 审核人 + 完整状态流转轨迹（谁在什么时候把状态从什么改成了什么）")
    @GetMapping("/{id}")
    public Result<AdminBookingDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(adminBookingService.detail(id));
    }

    @Operation(summary = "预约流转轨迹",
            description = "只要轨迹，用于详情页的侧边时间轴单独刷新")
    @GetMapping("/{id}/logs")
    public Result<List<BookingStatusLogVO>> logs(@PathVariable("id") Long id) {
        return Result.success(adminBookingService.statusLogs(id));
    }

    @Operation(summary = "强制取消预约",
            description = "管理员不受用户侧取消时限约束（用于场地变更、纠纷处理），cancel_by=3，归还名额并通知用户。"
                    + "走的是与用户取消、匠人取消同一条归还路径，名额账目不会出现两套算法")
    @OperationLog("强制取消预约")
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestBody @Valid BookingCancelDTO dto) {
        bookingService.adminCancel(dto);
        return Result.success();
    }

    @Operation(summary = "导出预约 Excel",
            description = "筛选条件与列表一致，最多 5000 条。含联系电话，便于改期时逐个通知")
    @GetMapping("/export")
    public void export(AdminBookingQueryDTO query, HttpServletResponse response) throws IOException {
        List<AdminBookingExcelVO> rows = adminBookingService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("预约列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), AdminBookingExcelVO.class)
                .sheet("预约列表")
                .doWrite(rows);
        log.info("[预约监管] 导出 {} 条", rows.size());
    }

    /* ------------------------- 定时任务手动触发 ------------------------- */

    @Operation(summary = "手动触发：超时未审自动拒绝",
            description = "等同于每 10 分钟的定时任务。先把 audit_timeout_hours 调小（0 表示立即超时）"
                    + "再触发，可快速验证整条链路；重复触发不会重复归还名额。"
                    + "自动拒绝会在流转轨迹里记为「系统」操作")
    @PostMapping("/task/auto-reject-timeout")
    public Result<Integer> autoRejectTimeout() {
        return Result.success(bookingService.autoRejectTimeout());
    }

    @Operation(summary = "手动触发：课程状态推进",
            description = "未开始 → 进行中 → 已结束，并把已核销的预约推进为已完成；重复触发不产生副作用")
    @PostMapping("/task/auto-finish-courses")
    public Result<Integer> autoFinishCourses() {
        return Result.success(bookingService.autoFinishCourses());
    }

    @Operation(summary = "手动触发：开课提醒",
            description = "给 24 小时内开课且已通过的用户发提醒；同一张预约只发一次（靠 message 表去重）")
    @PostMapping("/task/course-reminders")
    public Result<Integer> courseReminders() {
        return Result.success(bookingService.sendCourseReminders());
    }
}
