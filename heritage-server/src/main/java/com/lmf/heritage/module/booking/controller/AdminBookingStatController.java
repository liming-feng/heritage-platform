package com.lmf.heritage.module.booking.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.booking.service.AdminBookingService;
import com.lmf.heritage.module.booking.vo.AdminBookingStatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 · 预约统计（5 项）
 *
 * <p>每一项都能由一条独立的 SQL 直接复算：接口只做「分组结果 → 补零 / 算比例」
 * 这类不改变数值的加工，不在服务层做任何二次聚合。这样验收时
 * 「接口返回值 = 直查 SQL」是可验证的，而不是只能凭感觉相信。</p>
 *
 * <p>五组统计共用同一份口径（status IN (0,1,4,5) 为有效预约、IN (4,5) 为到场、
 * =1 为应到未到），与匠人看板 P7 完全一致 —— 两个后台对同一门课给出的
 * 热度排名与核销率必须相同，否则运营会不知道该信哪个。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-预约统计", description = "时段分布 / 爽约率 / 审核漏斗 / 课程热度 / 预约趋势")
@RestController
@RequestMapping("/admin/booking/stat")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminBookingStatController {

    private final AdminBookingService adminBookingService;

    @Operation(summary = "① 时段分布",
            description = "按课程开课时间落在 上午(06-12)/下午(12-18)/晚上(18-24)/凌晨(00-06) 聚合。"
                    + "四个时段固定返回，没有数据的补 0，柱状图不会少一根柱子")
    @GetMapping("/time-slot")
    public Result<AdminBookingStatVO.SlotDistribution> timeSlot() {
        return Result.success(adminBookingService.slotDistribution());
    }

    @Operation(summary = "② 爽约率",
            description = "只在已结束的课程上统计：实到 = 已核销 + 已结课，应到未到 = 已通过但未核销。"
                    + "一门已结束课程都没有时 absentRate 返回 null（而非 0%）。"
                    + "另附爽约人次最多的课程 Top N（只返回爽约数 > 0 的）")
    @GetMapping("/absent")
    public Result<AdminBookingStatVO.AbsentRate> absent(
            @RequestParam(value = "topN", required = false) Integer topN) {
        return Result.success(adminBookingService.absentRate(topN));
    }

    @Operation(summary = "③ 审核转化漏斗",
            description = "三段：提交 → 审核通过 → 到场核销，附通过率/到场率/整体转化率，"
                    + "并给出待审核、已拒绝、已取消的数量用于解释缺口来源")
    @GetMapping("/funnel")
    public Result<AdminBookingStatVO.AuditFunnel> funnel() {
        return Result.success(adminBookingService.auditFunnel());
    }

    @Operation(summary = "④ 课程热度榜",
            description = "排序主键为「占用名额之和」（与匠人看板同一口径），不是预约单数 —— "
                    + "一张单可能约多个名额。一个预约都没有的课程以 0 热度出现在榜上")
    @GetMapping("/course-heat")
    public Result<AdminBookingStatVO.CourseHeat> courseHeat(
            @RequestParam(value = "topN", required = false) Integer topN) {
        return Result.success(adminBookingService.courseHeat(topN));
    }

    @Operation(summary = "⑤ 近 N 天预约趋势",
            description = "按提交日期聚合，日期连续补零（缺格会把折线画成错误的斜线）。"
                    + "days 默认 30、上限 90。返回每日提交数、其中已通过数、名额数与星期")
    @GetMapping("/trend")
    public Result<AdminBookingStatVO.Trend> trend(
            @RequestParam(value = "days", required = false) Integer days) {
        return Result.success(adminBookingService.trend(days));
    }
}
