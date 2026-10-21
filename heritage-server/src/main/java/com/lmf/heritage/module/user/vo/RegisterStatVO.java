package com.lmf.heritage.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 注册量统计
 *
 * <p>三个口径放在一起返回，因为它们在管理页上是同一块看板的三个视图，
 * 分成三个接口只会让页面开三次请求：</p>
 * <ul>
 *   <li>{@code dayTrend} —— 近 N 天注册趋势（折线图，<b>无注册的日子补 0</b>）</li>
 *   <li>{@code monthTrend} —— 近 N 个月注册趋势（柱状图）</li>
 *   <li>{@code roleCount} —— 角色分布（饼图）</li>
 * </ul>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "注册量统计")
public class RegisterStatVO {

    @Schema(description = "累计注册用户数（不含已逻辑删除）")
    private Long total;

    @Schema(description = "今日新增")
    private Long todayNew;

    @Schema(description = "近 7 天新增")
    private Long weekNew;

    @Schema(description = "本月新增")
    private Long monthNew;

    @Schema(description = "累计匠人数")
    private Long craftsmanTotal;

    @Schema(description = "累计已通过资质审核的匠人数")
    private Long craftsmanPassed;

    @Schema(description = "待审核的匠人入驻申请数（管理端角标）")
    private Long craftsmanWaitAudit;

    @Schema(description = "统计口径说明中的天数（近 N 天）")
    private Integer days;

    @Schema(description = "统计口径说明中的月数（近 N 月）")
    private Integer months;

    @Schema(description = "近 N 天注册趋势，日期连续无缺口")
    private List<DayPoint> dayTrend;

    @Schema(description = "近 N 月注册趋势，月份连续无缺口")
    private List<MonthPoint> monthTrend;

    @Schema(description = "角色分布 [{role, roleText, count}]")
    private List<RoleCount> roleCount;

    @Data
    @Schema(description = "按天注册点")
    public static class DayPoint {
        @Schema(description = "日期 yyyy-MM-dd")
        private String date;
        @Schema(description = "日期文案 MM-dd，前端 X 轴直接用")
        private String dateText;
        @Schema(description = "星期文案")
        private String weekday;
        @Schema(description = "当天新增注册数")
        private Long count;
        @Schema(description = "当天是否有注册（前端据此高亮/标注）")
        private Boolean hasData;
    }

    @Data
    @Schema(description = "按月注册点")
    public static class MonthPoint {
        @Schema(description = "月份 yyyy-MM")
        private String month;
        @Schema(description = "当月新增注册数")
        private Long count;
    }

    @Data
    @Schema(description = "角色分布项")
    public static class RoleCount {
        @Schema(description = "角色码")
        private Integer role;
        @Schema(description = "角色文案")
        private String roleText;
        @Schema(description = "数量")
        private Long count;
    }
}
