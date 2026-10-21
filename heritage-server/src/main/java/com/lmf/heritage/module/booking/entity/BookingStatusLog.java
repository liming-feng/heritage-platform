package com.lmf.heritage.module.booking.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约状态流转轨迹
 *
 * <p>预约表上的 {@code audit_time} / {@code cancel_time} 只能保留「最后一次」结果，
 * 一条「驳回 → 改约 → 再通过 → 核销」的完整轨迹在上面看不出来。
 * 这张表按发生顺序逐条留痕，是纠纷处理与统计的原始依据。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("booking_status_log")
@Schema(description = "预约状态流转轨迹")
public class BookingStatusLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "预约 booking.id")
    private Long bookingId;

    /** 允许为 null：创建那一刻没有「前一个状态」 */
    @Schema(description = "原状态，null 表示创建")
    private Integer fromStatus;

    @Schema(description = "新状态 0待审核 1已通过 2已拒绝 3已取消 4已核销 5已完成")
    private Integer toStatus;

    @Schema(description = "操作人 user.id，系统操作为 null")
    private Long operatorId;

    @Schema(description = "操作方 1用户 2匠人 3管理员 4系统")
    private Integer operatorRole;

    @Schema(description = "操作人名称（冗余存储，操作人注销后仍可读）")
    private String operatorName;

    @Schema(description = "说明，如拒绝原因 / 取消原因")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "发生时间")
    private LocalDateTime createTime;
}
