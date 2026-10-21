package com.lmf.heritage.module.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 · 课程审核详情
 *
 * <p>比列表多出的部分都是审核时真正要看的：课程简介与详情富文本（判断内容是否违规）、
 * 匠人联系方式（沟通用）、以及<b>报名结构</b> —— 这门课已经有多少人报名、
 * 多少人核销、多少人应到未到。已开课的课程如果被下架，
 * 管理端必须一眼看到「影响多少个真实报名」。</p>
 *
 * <p>核销率沿用匠人看板的口径：只在课程已结束时才有意义，
 * 未结束返回 {@code null} 而不是 0% —— 「还没到核销时刻」和「核销率 0%」是两回事。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端课程审核详情")
public class AdminCourseDetailVO implements Serializable {

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

    @Schema(description = "课程简介")
    private String description;

    @Schema(description = "详情富文本")
    private String detail;

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

    @Schema(description = "匠人联系电话（审核沟通用）")
    private String craftsmanPhone;

    @Schema(description = "提交时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /* ---------------- 报名结构（按预约状态拆分） ---------------- */

    @Schema(description = "待审核报名数（status=0）")
    private Integer bookingWait;

    @Schema(description = "已通过未核销数（status=1）")
    private Integer bookingPassed;

    @Schema(description = "已拒绝数（status=2）")
    private Integer bookingRejected;

    @Schema(description = "已取消数（status=3）")
    private Integer bookingCanceled;

    @Schema(description = "已核销未结课数（status=4）")
    private Integer bookingCheckedIn;

    @Schema(description = "已结课数（status=5）")
    private Integer bookingFinished;

    @Schema(description = "实到人次 = 已核销 + 已结课")
    private Integer attendPeople;

    @Schema(description = "应到未到人次 = 已通过未核销")
    private Integer absentPeople;

    @Schema(description = "核销率（%），课程未结束时为 null")
    private Double checkinRate;
}
