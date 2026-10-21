package com.lmf.heritage.module.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项（含商品实时信息）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "购物车项")
public class CartItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车项 id")
    private Long id;

    @Schema(description = "商品 id")
    private Long goodsId;

    @Schema(description = "规格 id")
    private Long specId;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "是否勾选 0否 1是")
    private Integer checked;

    @Schema(description = "商品名称（实时）")
    private String goodsName;

    @Schema(description = "商品封面（实时）")
    private String cover;

    @Schema(description = "商品状态 0下架 1上架 2待审核")
    private Integer goodsStatus;

    @Schema(description = "所属匠人 id")
    private Long craftsmanId;

    @Schema(description = "规格名")
    private String specName;

    @Schema(description = "单价（有规格取规格价，否则取商品价）")
    private BigDecimal price;

    @Schema(description = "可用库存（有规格取规格库存）")
    private Integer stock;

    @Schema(description = "小计金额 = 单价 × 数量")
    private BigDecimal subtotal;

    @Schema(description = "是否失效 0有效 1失效（下架 / 删除 / 库存不足），失效项不可结算")
    private Integer invalid;

    @Schema(description = "加入时间")
    private LocalDateTime createTime;

    /** 计算小计，供 Service 组装时调用 */
    public void calcSubtotal() {
        if (price != null && quantity != null) {
            this.subtotal = price.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }
}
