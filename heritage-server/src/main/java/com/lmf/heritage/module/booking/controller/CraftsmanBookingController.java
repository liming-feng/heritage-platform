package com.lmf.heritage.module.booking.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.booking.dto.BookingAuditDTO;
import com.lmf.heritage.module.booking.dto.BookingCancelDTO;
import com.lmf.heritage.module.booking.dto.BookingCheckinDTO;
import com.lmf.heritage.module.booking.dto.BookingQueryDTO;
import com.lmf.heritage.module.booking.service.BookingService;
import com.lmf.heritage.module.booking.vo.BookingRosterExcelVO;
import com.lmf.heritage.module.booking.vo.BookingVO;
import com.lmf.heritage.module.booking.vo.CheckinResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 匠人端 · 预约管理（<b>题眼</b>）
 *
 * <p>覆盖「审核 → 核销」这条线下体验的闭环：待审核列表、通过、拒绝、报名名单、核销签到台。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "匠人端-预约", description = "预约审核 / 核销签到 / 报名名单")
@RestController
@RequestMapping("/craftsman/booking")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_CRAFTSMAN, Constants.ROLE_ADMIN})
public class CraftsmanBookingController {

    private final BookingService bookingService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Operation(summary = "预约列表",
            description = "只看自己课程的预约；支持状态 status、课程 courseId、关键词 keyword、"
                    + "超时未审 onlyTimeout=true 筛选、分页")
    @GetMapping("/list")
    public Result<PageResult<BookingVO>> list(BookingQueryDTO query) {
        return Result.success(bookingService.craftsmanBookings(query));
    }

    @Operation(summary = "各状态预约数", description = "返回 {状态码: 数量} + all 总数，用于 tab 角标与待办提醒")
    @GetMapping("/count")
    public Result<Map<String, Long>> count() {
        return Result.success(bookingService.craftsmanStatusCount());
    }

    @Operation(summary = "审核预约",
            description = "通过（approve=true）→ 状态变已通过并生成核销码；"
                    + "拒绝（approve=false）→ 必填拒绝原因，状态变已拒绝并归还名额。两种结果都会给用户发站内消息")
    @OperationLog("预约审核")
    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody @Valid BookingAuditDTO dto) {
        bookingService.audit(dto);
        return Result.success();
    }

    @Operation(summary = "核销签到",
            description = "支持两种方式，二选一：① 传 checkinCode 输入/扫码核销码；"
                    + "② 传 bookingId 在报名名单里直接点核销。"
                    + "成功后返回用户昵称、联系电话、预约时间、上课地点等信息供签到台核对。"
                    + "失败给出明确原因：5013 核销码不正确 / 5014 该预约已核销 / "
                    + "5015 该预约不属于你的课程 / 5009 预约不存在")
    @OperationLog("核销签到")
    @PostMapping("/checkin")
    public Result<CheckinResultVO> checkin(@RequestBody @Valid BookingCheckinDTO dto) {
        return Result.success(bookingService.checkin(dto));
    }

    @Operation(summary = "匠人取消预约", description = "如场地变更等，cancel_by=2，归还名额并通知用户")
    @OperationLog("匠人取消预约")
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestBody @Valid BookingCancelDTO dto) {
        bookingService.craftsmanCancel(dto);
        return Result.success();
    }

    @Operation(summary = "报名名单", description = "某门课程的全部报名记录，用于线下点名")
    @GetMapping("/roster")
    public Result<List<BookingVO>> roster(@RequestParam("courseId") Long courseId) {
        return Result.success(bookingService.roster(courseId));
    }

    @Operation(summary = "导出报名名单", description = "直接下载 Excel（.xlsx），列为预约单号/预约人/电话/名额/状态/核销码/签到时间/提交时间/备注")
    @GetMapping("/roster/export")
    public void exportRoster(@RequestParam("courseId") Long courseId, HttpServletResponse response)
            throws IOException {
        List<BookingVO> list = bookingService.roster(courseId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("报名名单", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<BookingRosterExcelVO> rows = new ArrayList<>(list.size());
        for (BookingVO b : list) {
            BookingRosterExcelVO row = new BookingRosterExcelVO();
            row.setBookingNo(b.getBookingNo());
            row.setContactName(b.getContactName());
            row.setContactPhone(b.getContactPhone());
            row.setQuota(b.getQuota());
            row.setStatusDesc(b.getStatusDesc());
            row.setCheckinCode(b.getCheckinCode());
            row.setCheckinTime(b.getCheckinTime() == null ? "" : TIME_FMT.format(b.getCheckinTime()));
            row.setCreateTime(b.getCreateTime() == null ? "" : TIME_FMT.format(b.getCreateTime()));
            row.setRemark(b.getRemark());
            rows.add(row);
        }
        EasyExcel.write(response.getOutputStream(), BookingRosterExcelVO.class)
                .sheet("报名名单")
                .doWrite(rows);
        log.info("[预约] 导出课程 {} 的报名名单，共 {} 条", courseId, rows.size());
    }
}
