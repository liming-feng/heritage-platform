package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表查询条件（前台）
 *
 * <p>GET 请求的查询参数会自动绑定到这个对象上。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品列表查询条件")
public class GoodsQueryDTO {

    @Schema(description = "分类 id，不传表示全部分类")
    private Long categoryId;

    @Schema(description = "关键词，匹配商品名称与副标题")
    private String keyword;

    @Schema(description = "价格区间下限")
    private BigDecimal minPrice;

    @Schema(description = "价格区间上限")
    private BigDecimal maxPrice;

    @Schema(description = "匠人 id，用于「该匠人的作品」")
    private Long craftsmanId;

    @Schema(description = "是否只看纯手工 1是 0否，不传表示不限")
    private Integer isHandmade;

    @Schema(description = "排序方式：default综合 / sales销量 / price_asc价格升 / price_desc价格降 / new最新")
    private String sort = "default";

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 12")
    private Long pageSize = 12L;

    /** 归一化分页参数，防越界 */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 12L;
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
