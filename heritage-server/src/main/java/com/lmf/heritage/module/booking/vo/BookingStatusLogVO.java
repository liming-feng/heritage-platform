package com.lmf.heritage.module.booking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约状态流转记录（管理端详情页展示）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "预约状态流转记录")
public class BookingStatusLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "记录 id")
    private Long id;

    @Schema(description = "原状态码，创建记录为 null")
    private Integer fromStatus;

    @Schema(description = "原状态文案，创建记录为空")
    private String fromStatusDesc;

    @Schema(description = "新状态码")
    private Integer toStatus;

    @Schema(description = "新状态文案")
    private String toStatusDesc;

    @Schema(description = "操作方 1用户 2匠人 3管理员 4系统")
    private Integer operatorRole;

    @Schema(description = "操作方文案")
    private String operatorRoleDesc;

    @Schema(description = "操作人名称，系统操作为「系统」")
    private String operatorName;

    @Schema(description = "说明，如拒绝原因 / 取消原因")
    private String remark;

    @Schema(description = "发生时间")
    private LocalDateTime createTime;
}
