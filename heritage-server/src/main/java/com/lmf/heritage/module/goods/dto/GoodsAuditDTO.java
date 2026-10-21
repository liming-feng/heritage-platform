package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端 · 商品审核
 *
 * <p>{@code pass=false} 时 {@code reason} 必填（服务层校验，错误码 8006）。
 * 驳回原因会原样展示给匠人，所以它是「给人看的」，不是日志。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品审核参数")
public class GoodsAuditDTO {

    @NotNull(message = "商品 id 不能为空")
    @Schema(description = "商品 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "审核结论不能为空")
    @Schema(description = "true 通过并上架 / false 驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean pass;

    @Schema(description = "驳回原因，pass=false 时必填")
    private String reason;
}
