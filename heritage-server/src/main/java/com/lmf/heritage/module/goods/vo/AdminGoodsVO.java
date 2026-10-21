package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 · 商品列表项
 *
 * <p>与匠人端的 {@code CraftsmanGoodsVO} 不合并：这边必须带出<b>匠人是谁</b>，
 * 审核列表上「一件不明来源的商品」是没法审的；那边则不需要（数据范围已经限定成自己）。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端商品列表项")
public class AdminGoodsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品 id")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "是否纯手工 0否 1是")
    private Integer isHandmade;

    @Schema(description = "商品状态 0下架 1在售 2待审核")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusDesc;

    @Schema(description = "审核状态 0待审核 1已通过 2已驳回")
    private Integer auditStatus;

    @Schema(description = "审核状态文案")
    private String auditStatusDesc;

    @Schema(description = "驳回 / 下架原因")
    private String rejectReason;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "分类名")
    private String categoryName;

    @Schema(description = "匠人 id")
    private Long craftsmanId;

    @Schema(description = "匠人姓名")
    private String craftsmanName;

    @Schema(description = "技艺门类")
    private String craftType;

    @Schema(description = "评分均值（无评价为 null）")
    private Double ratingAvg;

    @Schema(description = "评价数")
    private Integer reviewCount;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
