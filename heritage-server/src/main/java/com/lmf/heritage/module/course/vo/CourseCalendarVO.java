package com.lmf.heritage.module.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 日历排期中的一天
 *
 * <p><b>为什么按天分组而不是直接返回课程数组</b>：日历控件是「格子」结构，
 * 每个格子要么空白要么有课。如果后端只给一个平铺的课程列表，前端要自己遍历分组、
 * 还要处理「同一天多门课」的合并与排序。按天分好之后，前端拿到数组直接往格子上贴即可，
 * 分组规则只有服务端一份。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "日历排期中的一天")
public class CourseCalendarVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日期")
    private LocalDate date;

    @Schema(description = "日期文案，如「9月20日 周日」")
    private String dateText;

    @Schema(description = "当天课程数")
    private Integer courseCount;

    @Schema(description = "当天被预约的名额总数")
    private Integer bookedQuota;

    @Schema(description = "当天课程总名额")
    private Integer totalQuota;

    @Schema(description = "当天课程的预约满座率（0-100 的整数百分比），无名额时为 0")
    private Integer usedRate;

    @Schema(description = "当天的课程列表（按开课时间升序）")
    private List<CourseListVO> courses;
}
