package com.lmf.heritage.module.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单项视图（数据全部来自快照字段）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "订单项")
public class OrderItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单项 id")
    private Long id;

    @Schema(description = "商品 id")
    private Long goodsId;

    @Schema(description = "商品名称快照")
    private String goodsName;

    @Schema(description = "商品图片快照")
    private String goodsImage;

    @Schema(description = "规格名快照")
    private String specName;

    @Schema(description = "成交单价快照")
    private BigDecimal price;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "小计")
    private BigDecimal subtotal;

    @Schema(description = "是否已评价 0否 1是")
    private Integer reviewed;
}
