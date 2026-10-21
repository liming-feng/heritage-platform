package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存预警项
 *
 * <p>「预警」不是一个存下来的字段，而是 query 出来的：<b>在售（status=1 且审核通过）
 * 且库存 <= 阈值</b>。存字段会带来「阈值调整后历史数据不跟着变」的麻烦，
 * 也会出现「已下架商品还在预警列表里」的噪音。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "库存预警商品")
public class GoodsStockWarnVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品 id")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "封面图 URL")
    private String cover;

    @Schema(description = "当前库存")
    private Integer stock;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "累计销量")
    private Integer sales;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "预警等级：out 已售罄 / low 库存告急")
    private String warnLevel;

    @Schema(description = "预警文案")
    private String warnText;
}
