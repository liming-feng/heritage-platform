package com.lmf.heritage.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品销量 Top N（柱状图数据源）
 *
 * <p><b>销量为什么从 order_item 现算，而不是直接读 goods.sales</b>：
 * {@code goods.sales} 是商品详情页展示的「累计销量」，它是历史累计值、且包含已退款订单；
 * 看板要回答的是「这段时间哪些作品真的卖出去了」。所以这里按
 * {@code order_item.quantity} 在已支付订单上求和，并同时返回 {@code totalSales}
 * （goods 表的累计值）供对照——两个数字不一致是正常的，口径不同而已。</p>
 *
 * <p>商品名与图片取的是 {@code order_item} 里的下单快照，与订单详情页看到的完全一致；
 * 库存则要现查 {@code goods}，因为它才是「现在还剩几件」。
 * 商品可能已被删除，因此 {@code goods} 用的是 LEFT JOIN，删掉后库存显示为 null。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品销量排行项")
public class GoodsSalesTopVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "名次，从 1 开始")
    private Integer rank;

    @Schema(description = "商品 id")
    private Long goodsId;

    @Schema(description = "商品名（下单时的快照）")
    private String goodsName;

    @Schema(description = "商品图（下单时的快照）")
    private String goodsImage;

    @Schema(description = "所属分类名")
    private String categoryName;

    @Schema(description = "统计期成交件数")
    private Long salesQty;

    @Schema(description = "统计期成交金额（元）")
    private BigDecimal salesAmount;

    @Schema(description = "当前库存，商品已删除时为 null")
    private Integer stock;

    @Schema(description = "当前售价（元）")
    private BigDecimal price;

    @Schema(description = "商品表累计销量（对照用，口径与 salesQty 不同）")
    private Integer totalSales;
}
