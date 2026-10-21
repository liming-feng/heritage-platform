package com.lmf.heritage.module.course.controller;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.course.dto.CourseQueryDTO;
import com.lmf.heritage.module.course.service.CourseService;
import com.lmf.heritage.module.course.vo.CourseDetailVO;
import com.lmf.heritage.module.course.vo.CourseListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 · 体验课程（预约的载体）
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-课程", description = "课程列表 / 详情（剩余名额与预约截止倒计时）")
@RestController
@RequestMapping("/portal/course")
@RequiredArgsConstructor
public class PortalCourseController {

    private final CourseService courseService;

    @Operation(summary = "课程列表",
            description = "支持分类 categoryId、匠人 craftsmanId、关键词 keyword、状态 status、"
                    + "只看可预约 onlyBookable、排序 sort（default/price_asc/price_desc/new/hot）、分页。"
                    + "每条返回 remainSeconds 距预约截止的剩余秒数、canBook 是否可预约、closedReason 不可预约原因")
    @GetMapping("/list")
    public Result<PageResult<CourseListVO>> list(CourseQueryDTO query) {
        return Result.success(courseService.pageList(query));
    }

    @Operation(summary = "课程详情",
            description = "在列表字段基础上补：富文本详情、授课匠人卡片、评价条数与平均分；"
                    + "同时把 Redis 浏览量计数器 +1 并叠加到 viewCount 上")
    @GetMapping("/{id}")
    public Result<CourseDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(courseService.detail(id));
    }
}
