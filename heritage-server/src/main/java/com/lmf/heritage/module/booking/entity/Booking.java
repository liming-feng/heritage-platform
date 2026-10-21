package com.lmf.heritage.module.booking.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程预约（<b>题眼实体</b>）
 *
 * <p>这条记录同时承担三件事：</p>
 * <ol>
 *   <li><b>履约状态载体</b>：{@code status} 的 6 个取值由
 *       {@code BookingStatusEnum} 的状态机统一裁决，非法跳转一律拒绝；</li>
 *   <li><b>防重复预约的落点</b>：表上的唯一键 {@code uk_user_course(user_id, course_id)}
 *       是三层防超卖的**第三层**，即使前面两层全被绕过，数据库也不会出现同人同课的两条记录；</li>
 *   <li><b>线下核销凭据</b>：审核通过后生成 {@code checkinCode}，匠人端凭码签到。</li>
 * </ol>
 *
 * <p>关于 {@code uk_user_course} 与「重新预约」的关系：唯一键对已取消 / 已拒绝的记录同样生效，
 * 所以用户取消后**不能再插一条新记录**，只能把原来那条「复活」并重置状态。
 * 这也是本表没有走逻辑删除流程的原因——取消是「状态」，不是「删除」。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("booking")
@Schema(description = "课程预约（题眼）")
public class Booking implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "预约单号")
    private String bookingNo;

    @Schema(description = "课程 course.id")
    private Long courseId;

    @Schema(description = "预约用户 user.id")
    private Long userId;

    @Schema(description = "匠人 craftsman.id（冗余，便于匠人端一次查询）")
    private Long craftsmanId;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "预约名额数（含陪同）")
    private Integer quota;

    @Schema(description = "用户备注")
    private String remark;

    @Schema(description = "状态 0待审核 1已通过 2已拒绝 3已取消 4已核销 5已完成")
    private Integer status;

    @Schema(description = "核销码（审核通过后生成）")
    private String checkinCode;

    @Schema(description = "核销签到时间")
    private LocalDateTime checkinTime;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核人 user.id")
    private Long auditorId;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "取消发起方 1用户 2匠人 3管理员 4系统")
    private Integer cancelBy;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除 0未删 1已删")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "提交时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
