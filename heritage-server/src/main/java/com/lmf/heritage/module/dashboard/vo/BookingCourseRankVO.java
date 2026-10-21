package com.lmf.heritage.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约课程热度排行项
 *
 * <p>「热度」同时给出两个口径，因为它们回答的是不同问题：</p>
 * <ul>
 *   <li>{@code bookingCount} 预约单数 —— 有多少人点了「我要预约」，反映<b>吸引力</b>；</li>
 *   <li>{@code bookingPeople} 占用名额数（含陪同）—— 一门课实际占掉几个座位，反映<b>产能消耗</b>。</li>
 * </ul>
 * <p>只按单数排序会出现「一个人约 5 个名额」和「五个人各约 1 个名额」热度相同的情况，
 * 所以排序以名额数为主、单数为辅。</p>
 *
 * <p>排序口径与 {@link CourseQuotaVO} 的名额使用率保持同一套「占用名额」定义
 * （状态 0/1/4/5），两个图表放在一起看不会互相矛盾。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "预约课程热度排行项")
public class BookingCourseRankVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "名次，从 1 开始")
    private Integer rank;

    @Schema(description = "课程 id")
    private Long courseId;

    @Schema(description = "课程标题")
    private String courseTitle;

    @Schema(description = "课程封面")
    private String courseCover;

    @Schema(description = "开课时间")
    private LocalDateTime startTime;

    @Schema(description = "课程状态 0未开始 1进行中 2已结束")
    private Integer status;

    @Schema(description = "课程状态文案")
    private String statusDesc;

    @Schema(description = "预约单数")
    private Long bookingCount;

    @Schema(description = "占用名额数（含陪同）")
    private Long bookingPeople;

    @Schema(description = "总名额")
    private Integer totalQuota;

    @Schema(description = "剩余名额")
    private Integer remainQuota;

    @Schema(description = "名额使用率（0-100，1 位小数）")
    private Double usedRate;
}
