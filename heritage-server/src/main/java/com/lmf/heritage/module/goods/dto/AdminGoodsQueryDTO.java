package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 · 全平台商品列表查询条件
 *
 * <p>与匠人端 {@code CraftsmanGoodsQueryDTO} 的区别在于多了 {@code craftsmanId}：
 * 匠人端的数据范围由登录身份决定（不可能查别人的），管理端则要能「按匠人下钻」，
 * 排查某个匠人的商品是否合规。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端商品列表查询条件")
public class AdminGoodsQueryDTO {

    @Schema(description = "商品状态 0下架 1在售 2待审核")
    private Integer status;

    @Schema(description = "审核状态 0待审核 1已通过 2已驳回（含违规下架）")
    private Integer auditStatus;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "匠人 id（按匠人下钻）")
    private Long craftsmanId;

    @Schema(description = "关键词：匹配商品名 / 副标题 / 匠人真实姓名")
    private String keyword;

    @Schema(description = "价格下限（含）")
    private BigDecimal minPrice;

    @Schema(description = "价格上限（含）")
    private BigDecimal maxPrice;

    @Schema(description = "发布时间范围-起（含）")
    private LocalDateTime startTime;

    @Schema(description = "发布时间范围-止（含）")
    private LocalDateTime endTime;

    @Schema(description = "排序 new最新 / sales销量 / stock库存 / price价格 / view浏览；默认 new")
    private String sort;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }
        if (pageSize > 200) {
            pageSize = 200L;
        }
        if (keyword != null) {
            String t = keyword.trim();
            keyword = t.isEmpty() ? null : t;
        }
        if (sort == null || sort.isBlank()) {
            sort = "new";
        }
    }
}
