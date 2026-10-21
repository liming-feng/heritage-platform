package com.lmf.heritage.module.feedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端 · 反馈处理时效统计
 *
 * <p><b>处理时长的口径</b>：从「提交时间」到 {@code handle_time}，只在已完成的反馈上统计。
 * 把「处理中」的也算进来会得到一个被无限拉长的平均值 —— 那些还没处理完的
 * 反馈时长每天都在涨，它不该影响对已完成工作的评价。</p>
 *
 * <p>24 小时完成率同理只看已完成的部分：它是「处理得快不快」的指标，
 * 不与「有没有积压」混在一起。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "反馈处理时效统计")
public class FeedbackStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "反馈总量")
    private Long total;

    @Schema(description = "待处理数量")
    private Long waitCount;

    @Schema(description = "处理中数量")
    private Long processingCount;

    @Schema(description = "已完成数量")
    private Long doneCount;

    @Schema(description = "完成率 %（已完成 / 总量）")
    private BigDecimal doneRate;

    @Schema(description = "已完成的反馈中，有处理完成时间的条数")
    private Long handledCount;

    @Schema(description = "平均处理时长（小时）；没有已完成的反馈时为 null")
    private BigDecimal avgHandleHours;

    @Schema(description = "最短处理时长（小时）")
    private BigDecimal minHandleHours;

    @Schema(description = "最长处理时长（小时）")
    private BigDecimal maxHandleHours;

    @Schema(description = "24 小时内完成的条数")
    private Long within24hCount;

    @Schema(description = "24 小时内完成率 %（24 小时内完成 / 已完成）")
    private BigDecimal within24hRate;

    @Schema(description = "按反馈类型分组的统计")
    private List<TypeStat> typeStats;

    @Schema(description = "近 N 天每日提交量与完成量（日期连续，缺失补 0）")
    private List<DailyStat> daily;

    @Data
    @Schema(description = "按类型分组的反馈统计")
    public static class TypeStat implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "类型 1功能建议 2内容纠错 3投诉举报 4其他")
        private Integer type;

        @Schema(description = "类型描述")
        private String typeDesc;

        @Schema(description = "该类型的反馈总数")
        private Long count;

        @Schema(description = "其中已完成的条数")
        private Long doneCount;

        @Schema(description = "该类型平均处理时长（小时）")
        private BigDecimal avgHandleHours;
    }

    @Data
    @Schema(description = "每日反馈统计")
    public static class DailyStat implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "日期 yyyy-MM-dd")
        private String date;

        @Schema(description = "当日提交量")
        private Long submitCount;

        @Schema(description = "当日完成量（按处理完成时间归属当日，不是按提交日）")
        private Long doneCount;
    }
}
