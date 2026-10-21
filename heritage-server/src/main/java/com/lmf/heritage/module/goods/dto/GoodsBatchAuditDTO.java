package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 商品批量审核
 *
 * <p>批量采用<b>整批回滚</b>语义：批内只要有任意一件不是「待审核」状态，
 * 整批拒绝、不做部分成功。理由和匠人端批量发货一致 ——
 * 部分成功会让操作者以为全批都过了，而实际上只过了一部分，
 * 剩下的还躺在待审队列里等着被发现。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品批量审核参数")
public class GoodsBatchAuditDTO {

    @NotEmpty(message = "请至少选择一件商品")
    @Size(max = 50, message = "单次最多审核 50 件")
    @Schema(description = "商品 id 列表，单次最多 50 个", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @NotNull(message = "审核结论不能为空")
    @Schema(description = "true 全部通过并上架 / false 全部驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean pass;

    @Schema(description = "驳回原因，pass=false 时必填")
    private String reason;
}
