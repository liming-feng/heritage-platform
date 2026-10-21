package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 匠人端商品发布 / 编辑请求
 *
 * <p>不传 {@code id} 为发布，传 {@code id} 为编辑（编辑时会再校验这件商品是否属于当前匠人）。</p>
 *
 * <p><b>不存在 status 字段是刻意的</b>：上下架有独立接口。如果这里也允许传状态，
 * 就会出现「发布时直接传 status=1 上架」绕过审核的漏洞。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品发布/编辑请求")
public class GoodsSaveDTO {

    @Schema(description = "商品 id，不传表示发布新商品")
    private Long id;

    @NotNull(message = "请选择商品分类")
    @Schema(description = "分类 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @NotBlank(message = "请填写商品名称")
    @Size(max = 100, message = "商品名称最长 100 个字符")
    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 150, message = "副标题最长 150 个字符")
    @Schema(description = "副标题 / 卖点")
    private String subtitle;

    @NotBlank(message = "请上传商品封面")
    @Size(max = 255, message = "封面地址最长 255 个字符")
    @Schema(description = "封面图 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cover;

    @Size(max = 500, message = "简要描述最长 500 个字符")
    @Schema(description = "简要描述")
    private String description;

    @Schema(description = "详情（富文本）")
    private String detail;

    @NotNull(message = "请填写售价")
    @DecimalMin(value = "0.01", message = "售价必须大于 0")
    @Schema(description = "售价", requiredMode = Schema.RequiredMode.REQUIRED, example = "299.00")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "原价不能为负数")
    @Schema(description = "原价（划线价），不传表示不做划线")
    private BigDecimal originalPrice;

    @NotNull(message = "请填写库存")
    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 999999, message = "库存超出合理范围")
    @Schema(description = "库存", requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
    private Integer stock;

    @Schema(description = "是否纯手工 0否 1是，不传默认 1", example = "1")
    private Integer isHandmade;
}
