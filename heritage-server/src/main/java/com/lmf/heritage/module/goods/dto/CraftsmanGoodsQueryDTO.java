package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 匠人端商品列表查询条件
 *
 * <p>与前台 {@link GoodsQueryDTO} 的关键差别：这里不过滤上架与审核状态，
 * 匠人必须能看到自己「待审核」「已驳回」「已下架」的全部商品，
 * 否则无法处理驳回重提与重新上架。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人端商品列表查询条件")
public class CraftsmanGoodsQueryDTO {

    @Schema(description = "商品状态 0下架 1上架 2待审核，不传表示全部")
    private Integer status;

    @Schema(description = "审核状态 0待审核 1通过 2驳回，不传表示全部")
    private Integer auditStatus;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "关键词，匹配商品名称与副标题")
    private String keyword;

    @Schema(description = "只看库存预警商品（在售且库存 <= 5）")
    private Boolean stockWarn;

    /**
     * 库存预警阈值
     *
     * <p>不暴露给前端（没有 @Schema 就仍会被绑定，但前端不会传）：
     * 由服务层从 {@code Constants.STOCK_WARN_THRESHOLD} 填进来，
     * 目的是让阈值只有一个定义处，SQL 只负责比较。</p>
     */
    private Integer stockWarnThreshold = 5;

    @Schema(description = "排序：default最新 / sales销量 / stock库存升 / view浏览量")
    private String sort = "default";

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    /** 归一化分页与关键词，防越界 */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }
        if (pageSize > 100) {
            pageSize = 100L;
        }
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }
        if (sort == null || sort.isBlank()) {
            sort = "default";
        }
    }
}
