package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入购物车请求
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "加入购物车请求")
public class CartAddDTO {

    @NotNull(message = "商品不能为空")
    @Schema(description = "商品 id")
    private Long goodsId;

    @Schema(description = "规格 id，无规格商品可不传")
    private Long specId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 999, message = "单个商品最多购买 999 件")
    @Schema(description = "数量", example = "1")
    private Integer quantity;
}
