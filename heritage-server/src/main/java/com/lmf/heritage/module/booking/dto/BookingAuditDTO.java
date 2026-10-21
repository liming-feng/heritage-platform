package com.lmf.heritage.module.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 匠人审核预约请求
 *
 * <p>拒绝时 {@code rejectReason} 必填，在业务层校验（而不是用 {@code @NotBlank} 一刀切），
 * 因为「通过」时并不需要填原因。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "审核预约请求")
public class BookingAuditDTO {

    @NotNull(message = "请指定要审核的预约")
    @Schema(description = "预约 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookingId;

    @NotNull(message = "请指定审核结果")
    @Schema(description = "true 通过 / false 拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean approve;

    @Schema(description = "拒绝原因（拒绝时必填）")
    private String rejectReason;
}
