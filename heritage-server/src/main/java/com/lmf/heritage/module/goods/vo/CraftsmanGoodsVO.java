package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 匠人端商品列表项
 *
 * <p>比前台 {@code GoodsListVO} 多出状态、审核、驳回原因与预警标记：
 * 匠人看的是「我的经营台账」，重点在「哪些该处理」，而不是「哪些好看」。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人端商品列表项")
public class CraftsmanGoodsVO implements Serializable {

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

    @Schema(description = "分类名称")
    private String categoryName;

    /* ---------------- 状态与运营 ---------------- */

    @Schema(description = "状态 0下架 1上架 2待审核")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusDesc;

    @Schema(description = "审核状态 0待审核 1通过 2驳回")
    private Integer auditStatus;

    @Schema(description = "审核状态名称")
    private String auditStatusDesc;

    @Schema(description = "驳回原因（仅驳回时有值）")
    private String rejectReason;

    @Schema(description = "累计浏览量（含 Redis 中尚未回写的增量）")
    private Integer viewCount;

    @Schema(description = "是否库存预警（在售且库存 <= 阈值）")
    private Boolean stockWarn;

    @Schema(description = "评价平均分，无评价时为 null")
    private Double ratingAvg;

    @Schema(description = "评价条数")
    private Integer reviewCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
