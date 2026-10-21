package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改购物车数量请求
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "修改购物车数量请求")
public class CartUpdateDTO {

    @NotNull(message = "购物车项 id 不能为空")
    @Schema(description = "购物车项 id")
    private Long id;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 999, message = "单个商品最多购买 999 件")
    @Schema(description = "新数量")
    private Integer quantity;
}
