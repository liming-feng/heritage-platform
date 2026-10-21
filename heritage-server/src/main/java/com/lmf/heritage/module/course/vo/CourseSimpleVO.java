package com.lmf.heritage.module.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程简要信息（首页体验活动预告、课程列表用）
 *
 * <p>题眼相关字段：{@code remainQuota} 剩余名额、{@code bookingDeadline} 预约截止时间，
 * 前端据此显示「还剩 N 个名额」「剩余 X 天截止」。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "课程简要信息")
public class CourseSimpleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "课程 id")
    private Long id;

    @Schema(description = "课程标题")
    private String title;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "课程费用")
    private BigDecimal price;

    @Schema(description = "上课地点")
    private String address;

    @Schema(description = "开课时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "预约截止时间")
    private LocalDateTime bookingDeadline;

    @Schema(description = "总名额")
    private Integer totalQuota;

    @Schema(description = "剩余名额")
    private Integer remainQuota;

    @Schema(description = "难度 1入门 2进阶 3高级")
    private Integer difficulty;

    @Schema(description = "状态 0未开始 1进行中 2已结束")
    private Integer status;

    @Schema(description = "授课匠人 id")
    private Long craftsmanId;

    @Schema(description = "授课匠人姓名")
    private String craftsmanName;

    @Schema(description = "匠人技艺门类")
    private String craftType;
}
