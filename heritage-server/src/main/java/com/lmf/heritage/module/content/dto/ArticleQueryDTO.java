package com.lmf.heritage.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资讯查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "资讯查询条件")
public class ArticleQueryDTO {

    @Schema(description = "分类 category.id，不传表示全部")
    private Long categoryId;

    @Schema(description = "关键词，匹配标题 / 摘要 / 作者")
    private String keyword;

    @Schema(description = "标签，命中 article.tags 中的某一项即可（模糊匹配）")
    private String tag;

    @Schema(description = "排序：latest 最新（默认，置顶优先） / hot 最热（按浏览量）")
    private String sort = "latest";

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    /** 归一化分页与排序参数，避免前端传进非法值把 SQL 带歪 */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }
        if (pageSize > 50) {
            pageSize = 50L;
        }
        if (!"hot".equals(sort)) {
            sort = "latest";
        }
        if (keyword != null) {
            keyword = keyword.trim();
        }
        if (tag != null) {
            tag = tag.trim();
        }
    }
}
