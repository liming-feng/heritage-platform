package com.lmf.heritage.module.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端 · 资讯列表项
 *
 * <p>与用户端 {@code ArticleListVO} 的差异：多了 {@code status / statusDesc / updateTime}，
 * 少了「只查已发布」这个前提。管理端列表里必须能直接看出哪篇是草稿，
 * 而不是点进去才知道。</p>
 *
 * <p>同样<b>不带正文 content</b>：富文本正文几十 KB 起，列表页用不到。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端资讯列表项")
public class AdminArticleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "是否置顶 0否 1是")
    private Integer isTop;

    @Schema(description = "置顶描述，如「置顶」/「—」")
    private String isTopDesc;

    @Schema(description = "状态 0草稿或下架 1已发布")
    private Integer status;

    @Schema(description = "状态描述，如「已发布」/「草稿」")
    private String statusDesc;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "最后编辑时间")
    private LocalDateTime updateTime;
}
