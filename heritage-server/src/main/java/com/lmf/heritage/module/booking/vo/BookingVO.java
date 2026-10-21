package com.lmf.heritage.module.booking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约记录视图（用户端「我的预约」与匠人端「预约管理」共用）
 *
 * <p>用一份 VO 承载两端：匠人端多用到 {@code buyerName}、{@code courseTitle}，
 * 用户端多用到 {@code checkinCode}。按端裁剪反而容易让两边的字段名漂移，
 * 保持一份、由服务端把按钮开关算好，前端更不容易写错。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "预约记录")
public class BookingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /* ---------------- 预约本身 ---------------- */

    @Schema(description = "预约 id")
    private Long id;

    @Schema(description = "预约单号")
    private String bookingNo;

    @Schema(description = "状态 0待审核 1已通过 2已拒绝 3已取消 4已核销 5已完成")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusDesc;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "预约名额数")
    private Integer quota;

    @Schema(description = "用户备注")
    private String remark;

    @Schema(description = "核销码（仅已通过 / 已核销时有值）")
    private String checkinCode;

    @Schema(description = "核销签到时间")
    private LocalDateTime checkinTime;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "取消发起方 1用户 2匠人 3管理员 4系统")
    private Integer cancelBy;

    @Schema(description = "取消发起方名称")
    private String cancelByDesc;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "提交时间")
    private LocalDateTime createTime;

    /* ---------------- 课程信息（下单时的实时引用） ---------------- */

    @Schema(description = "课程 id")
    private Long courseId;

    @Schema(description = "课程标题")
    private String courseTitle;

    @Schema(description = "课程封面")
    private String courseCover;

    @Schema(description = "上课地点")
    private String courseAddress;

    @Schema(description = "开课时间")
    private LocalDateTime courseStartTime;

    @Schema(description = "结束时间")
    private LocalDateTime courseEndTime;

    @Schema(description = "课程费用")
    private BigDecimal coursePrice;

    @Schema(description = "预约截止时间")
    private LocalDateTime courseBookingDeadline;

    @Schema(description = "可取消截止时间")
    private LocalDateTime courseCancelDeadline;

    /* ---------------- 双方信息 ---------------- */

    @Schema(description = "用户 id")
    private Long userId;

    @Schema(description = "预约人昵称（匠人端展示用）")
    private String buyerName;

    @Schema(description = "匠人 id")
    private Long craftsmanId;

    @Schema(description = "匠人姓名")
    private String craftsmanName;

    @Schema(description = "匠人技艺门类")
    private String craftType;

    /* ---------------- 按钮开关（后端算好，前端不重写规则） ---------------- */

    @Schema(description = "用户端：是否可取消")
    private Boolean canCancel;

    @Schema(description = "用户端：是否可查看核销码")
    private Boolean canShowCheckinCode;

    @Schema(description = "用户端：是否可查看课程详情")
    private Boolean canViewCourse;

    @Schema(description = "用户端：课程结束后是否可评价")
    private Boolean canReview;

    @Schema(description = "匠人端：是否可审核（待审核）")
    private Boolean canAudit;

    @Schema(description = "匠人端：是否可核销（已通过）")
    private Boolean canCheckin;

    @Schema(description = "匠人端：是否已超时未审")
    private Boolean timeout;

    @Schema(description = "不可取消的原因（可取消时为空）")
    private String cancelDisabledReason;
}
