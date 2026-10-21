package com.lmf.heritage.module.course.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.course.dto.CourseQueryDTO;
import com.lmf.heritage.module.course.dto.CourseSaveDTO;
import com.lmf.heritage.module.course.service.CourseService;
import com.lmf.heritage.module.course.vo.CourseCalendarVO;
import com.lmf.heritage.module.course.vo.CourseListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 匠人端 · 课程管理
 *
 * <p>路径前缀 {@code /craftsman} 由拦截器做角色隔离；每个操作还会再校验
 * 「课程是否属于当前登录匠人」，防止横向越权改别人的课程。</p>
 *
 * <p><b>关于课程状态</b>：{@code status}（未开始 / 进行中 / 已结束）由定时任务按
 * {@code start_time} / {@code end_time} 自动推进，不由前端传参决定——「课到底开没开」
 * 是一个事实。所以这里的「状态管理」管的是两件真正由人决定的事：<br>
 * ① 预约开关（提前截止或重新开放预约）；<br>
 * ② 提前结束课程（场地变更、报名不足）。</p>
 *
 * <p><b>报名名单导出</b>属于某门课程的报名数据，接口放在预约模块：
 * {@code GET /craftsman/booking/roster/export?courseId=xxx}。
 * 这里不重复提供一个同义接口，避免同一份导出逻辑出现两个入口。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "匠人端-课程", description = "课程 CRUD / 状态管理 / 日历排期")
@RestController
@RequestMapping("/craftsman/course")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_CRAFTSMAN, Constants.ROLE_ADMIN})
public class CraftsmanCourseController {

    private final CourseService courseService;

    @Operation(summary = "我的课程", description = "只看自己的课程，含未开始、进行中与已结束的全部状态")
    @GetMapping("/list")
    public Result<PageResult<CourseListVO>> list(CourseQueryDTO query) {
        return Result.success(courseService.myCourses(query));
    }

    @Operation(summary = "新增/编辑课程",
            description = "不传 id 为新增（剩余名额自动等于总名额）；传 id 为编辑。"
                    + "改小总名额时会校验不能小于已预约数，并同步调整剩余名额")
    @OperationLog("保存课程")
    @PostMapping("/save")
    public Result<Long> save(@RequestBody @Valid CourseSaveDTO dto) {
        return Result.success(courseService.saveCourse(dto));
    }

    @Operation(summary = "删除课程", description = "还有待审核 / 已通过的预约时不允许删除")
    @OperationLog("删除课程")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        courseService.removeCourse(id);
        return Result.success();
    }

    @Operation(summary = "日历排期视图",
            description = "按天分组返回区间内开课的课程，含当天课程数、已约名额、总名额与满座率。"
                    + "日期格式 yyyy-MM-dd，区间为闭区间且不超过 366 天，未传时默认查本月")
    @GetMapping("/calendar")
    public Result<List<CourseCalendarVO>> calendar(
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        // 不传就默认查本月：日历打开时总得有个默认视野，让前端少一次判断
        LocalDate today = LocalDate.now();
        LocalDate from = start != null ? start : today.withDayOfMonth(1);
        LocalDate to = end != null ? end : from.plusMonths(1).minusDays(1);
        return Result.success(courseService.calendar(from, to));
    }

    @Operation(summary = "开放 / 关闭预约",
            description = "open=true 重新开放预约（截止时间顶到开课时间）；"
                    + "open=false 立即关闭预约（截止时间设为当前时刻）。"
                    + "已开课或已结束的课程返回 5019")
    @OperationLog("调整课程预约开关")
    @PostMapping("/{id}/booking")
    public Result<Void> booking(@PathVariable("id") Long id,
                               @RequestParam("open") Boolean open) {
        courseService.setBookingOpen(id, Boolean.TRUE.equals(open));
        return Result.success();
    }

    @Operation(summary = "提前结束课程",
            description = "用于场地变更、报名不足等场景。已结束的课程重复调用返回 5002。"
                    + "已核销的预约会在下一次定时任务扫描时推进为已完成")
    @OperationLog("提前结束课程")
    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable("id") Long id) {
        courseService.finishCourse(id);
        return Result.success();
    }
}
