package com.lmf.heritage.module.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车整体视图（列表 + 汇总）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "购物车")
public class CartVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车项列表")
    private List<CartItemVO> items;

    @Schema(description = "商品种类数")
    private Integer totalCount;

    @Schema(description = "已勾选种类数")
    private Integer checkedCount;

    @Schema(description = "已勾选商品总件数")
    private Integer checkedQuantity;

    @Schema(description = "已勾选商品总金额")
    private BigDecimal checkedAmount;

    @Schema(description = "勾选项是否全部可用（有失效项时为 false，前端禁止结算）")
    private Boolean allValid;
}
