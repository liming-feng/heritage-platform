package com.lmf.heritage.module.goods.vo;

import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import com.lmf.heritage.module.review.vo.ReviewVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情
 *
 * <p>按 P3 要求一次性返回：基本信息 + 图集 + 规格 + 匠人简介 + 评价列表。
 * 评价列表只带最近若干条，完整分页走 {@code /portal/review/list}。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品详情")
public class GoodsDetailVO implements Serializable {

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

    @Schema(description = "简要描述")
    private String description;

    @Schema(description = "详情富文本")
    private String detail;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价（划线价）")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "浏览量（含 Redis 未回写的增量）")
    private Integer viewCount;

    @Schema(description = "是否纯手工 0否 1是")
    private Integer isHandmade;

    @Schema(description = "上架时间")
    private LocalDateTime createTime;

    @Schema(description = "分类")
    private CategoryVO category;

    @Schema(description = "图集（含封面，按 sort 排序）")
    private List<String> images;

    @Schema(description = "规格列表")
    private List<GoodsSpecVO> specs;

    @Schema(description = "所属匠人简介")
    private CraftsmanSimpleVO craftsman;

    @Schema(description = "评价平均分")
    private Double ratingAvg;

    @Schema(description = "评价总条数")
    private Integer reviewCount;

    @Schema(description = "最近评价（默认 5 条）")
    private List<ReviewVO> reviews;
}
