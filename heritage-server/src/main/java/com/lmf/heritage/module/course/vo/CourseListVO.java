package com.lmf.heritage.module.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程列表项
 *
 * <p><b>题眼相关</b>：{@code remainQuota}（剩余名额）与 {@code remainSeconds}
 * （距预约截止的剩余秒数）都由服务端算好，前端只负责显示。
 * 倒计时交给前端自己用 {@code startTime} 算会出现「客户端时钟不准」的问题，
 * 所以这里额外返回一个服务端计算的剩余秒数。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "课程列表项")
public class CourseListVO implements Serializable {

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

    @Schema(description = "可取消截止时间")
    private LocalDateTime cancelDeadline;

    @Schema(description = "总名额")
    private Integer totalQuota;

    @Schema(description = "剩余名额")
    private Integer remainQuota;

    @Schema(description = "已预约名额数（= 总名额 - 剩余名额）")
    private Integer bookedQuota;

    @Schema(description = "课时（分钟）")
    private Integer duration;

    @Schema(description = "难度 1入门 2进阶 3高级")
    private Integer difficulty;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "累计预约数")
    private Integer bookingCount;

    @Schema(description = "状态 0未开始 1进行中 2已结束")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusDesc;

    /**
     * 平台审核状态。
     *
     * <p>只有匠人端「我的课程」的 SQL 会查这一列（前台列表固定只出 audit_status=1 的课），
     * 所以前台的列表项里这两个字段是 null，序列化时保留但为空即可。</p>
     */
    @Schema(description = "审核状态 0待审核 1已通过 2已驳回")
    private Integer auditStatus;

    @Schema(description = "审核状态名称")
    private String auditStatusDesc;

    @Schema(description = "授课匠人 id")
    private Long craftsmanId;

    @Schema(description = "授课匠人姓名")
    private String craftsmanName;

    @Schema(description = "匠人技艺门类")
    private String craftType;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    /* ---------------- 倒计时与可预约判定（服务端算好） ---------------- */

    @Schema(description = "距预约截止的剩余秒数，<=0 表示已截止")
    private Long remainSeconds;

    @Schema(description = "是否还能预约")
    private Boolean canBook;

    @Schema(description = "不能预约的原因（可预约时为空）")
    private String closedReason;
}
