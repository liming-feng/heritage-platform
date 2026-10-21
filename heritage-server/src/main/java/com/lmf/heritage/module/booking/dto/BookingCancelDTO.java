package com.lmf.heritage.module.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消预约请求
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "取消预约请求")
public class BookingCancelDTO {

    @NotNull(message = "请指定要取消的预约")
    @Schema(description = "预约 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookingId;

    @Schema(description = "取消原因")
    private String reason;
}
