package com.lmf.heritage.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 · 资讯列表查询条件
 *
 * <p>与用户端 {@code ArticleQueryDTO} 的关键区别是<b>不默认过滤 status</b>：
 * 用户端只能看到已发布的（草稿漏出去就是事故），而管理端的草稿箱本身就是
 * 要被翻出来继续写的。两套查询条件分开，比在一条 SQL 上挂一个「要不要过滤状态」
 * 的开关更容易确认谁在什么时候能看见什么。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端资讯查询条件")
public class AdminArticleQueryDTO {

    @Schema(description = "关键词：匹配标题 / 作者 / 摘要")
    private String keyword;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "状态 0草稿或已下架 1已发布；不传表示全部")
    private Integer status;

    @Schema(description = "是否置顶 0否 1是；不传表示全部")
    private Integer isTop;

    @Schema(description = "发布时间范围-起（含）")
    private LocalDateTime startTime;

    @Schema(description = "发布时间范围-止（含）")
    private LocalDateTime endTime;

    @Schema(description = "排序 new最新发布(默认) / view浏览量 / like点赞数 / comment评论数")
    private String sort;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    /** 勾选导出：只导出这几篇资讯；为空表示按当前筛选条件导出。列表接口忽略该参数 */
    @Schema(description = "勾选导出的资讯 id（逗号分隔或重复参数）；不传表示按筛选条件导出")
    private java.util.List<Long> ids;

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
