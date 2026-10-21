package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 商品违规下架
 *
 * <p><b>为什么「违规下架」不能只把 status 改成 0</b>：{@code updateShelf} 的准入条件是
 * 「已通过审核」，匠人自己一次点击就能把商品重新上架 —— 平台的下架决定被绕过去了。
 * 所以这里把 {@code audit_status} 一并置为「已驳回」并把原因写进 {@code reject_reason}，
 * 匠人必须先改稿、再走一次审核才能重新上架。下架是结论，不是开关。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品违规下架参数")
public class GoodsOffShelfDTO {

    @NotEmpty(message = "请至少选择一件商品")
    @Size(max = 50, message = "单次最多下架 50 件")
    @Schema(description = "商品 id 列表，单次最多 50 个", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @NotBlank(message = "请填写下架原因")
    @Size(max = 200, message = "下架原因不能超过 200 字")
    @Schema(description = "违规下架原因（必填，会展示给匠人）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
