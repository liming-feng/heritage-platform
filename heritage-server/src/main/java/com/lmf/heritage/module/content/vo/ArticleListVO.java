package com.lmf.heritage.module.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资讯列表项
 *
 * <p>列表刻意<b>不返回正文 content</b>：一篇富文本正文动辄几十 KB，
 * 一页 10 条就是几百 KB，而列表页只用到标题与摘要。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "资讯列表项")
public class ArticleListVO implements Serializable {

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

    @Schema(description = "发布时间")
    private LocalDateTime createTime;
}
