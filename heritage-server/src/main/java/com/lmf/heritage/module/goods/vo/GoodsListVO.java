package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品列表项（列表页只需要这些字段，详情字段不在这里返回，避免列表接口数据过大）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品列表项")
public class GoodsListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品 id")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题 / 卖点")
    private String subtitle;

    @Schema(description = "封面图 URL")
    private String cover;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价（划线价）")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "是否纯手工 0否 1是")
    private Integer isHandmade;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "分类名称（联表带出）")
    private String categoryName;

    @Schema(description = "匠人 id")
    private Long craftsmanId;

    @Schema(description = "匠人姓名（联表带出）")
    private String craftsmanName;

    @Schema(description = "匠人技艺门类（联表带出）")
    private String craftType;

    @Schema(description = "评价平均分，无评价时为 null")
    private Double ratingAvg;

    @Schema(description = "评价条数")
    private Integer reviewCount;
}
