package com.lmf.heritage.module.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 · 课程列表项
 *
 * <p>沿用前台 {@code CourseListVO} 的名额口径（总名额 / 剩余 / 已约），
 * 并补上审核相关的三个字段。管理端看的是「这门课提交上来合不合规」，
 * 因此还带出提交时间与匠人信息。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端课程列表项")
public class AdminCourseVO implements Serializable {

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

    @Schema(description = "课程价格")
    private BigDecimal price;

    @Schema(description = "上课地点")
    private String address;

    @Schema(description = "开课时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "预约截止时间")
    private LocalDateTime bookingDeadline;

    @Schema(description = "取消截止时间")
    private LocalDateTime cancelDeadline;

    @Schema(description = "总名额")
    private Integer totalQuota;

    @Schema(description = "剩余名额")
    private Integer remainQuota;

    @Schema(description = "已约名额（= 总名额 − 剩余名额）")
    private Integer bookedQuota;

    @Schema(description = "名额使用率，保留 1 位小数的百分数")
    private Double usedRate;

    @Schema(description = "课程时长（分钟）")
    private Integer duration;

    @Schema(description = "难度 1入门 2进阶 3高阶")
    private Integer difficulty;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "预约数")
    private Integer bookingCount;

    @Schema(description = "课程状态 0未开始 1进行中 2已结束")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusDesc;

    @Schema(description = "审核状态 0待审核 1已通过 2已驳回")
    private Integer auditStatus;

    @Schema(description = "审核状态文案")
    private String auditStatusDesc;

    @Schema(description = "驳回 / 下架原因")
    private String rejectReason;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "分类名")
    private String categoryName;

    @Schema(description = "匠人 id")
    private Long craftsmanId;

    @Schema(description = "匠人姓名")
    private String craftsmanName;

    @Schema(description = "技艺门类")
    private String craftType;

    @Schema(description = "提交时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
