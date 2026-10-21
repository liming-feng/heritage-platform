package com.lmf.heritage.module.craftsman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端 · 匠人资质审核
 *
 * <p>{@code pass=false} 时 {@code reason} 必填 —— 校验不放在注解上而在服务层做，
 * 因为「驳回必填原因、通过不需要」是条件校验，Bean Validation 表达不了。
 * 服务层用 {@code AUDIT_REASON_REQUIRED(8006)} 拦。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人资质审核参数")
public class CraftsmanAuditDTO {

    @NotNull(message = "匠人 id 不能为空")
    @Schema(description = "匠人 id（craftsman 表主键，非 user id）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "审核结论不能为空")
    @Schema(description = "true 通过 / false 驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean pass;

    @Schema(description = "驳回原因，pass=false 时必填")
    private String reason;
}
