package com.lmf.heritage.module.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯详情
 *
 * <p>比列表多三样东西：正文、当前用户的点赞/收藏状态、相关推荐。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "资讯详情")
public class ArticleDetailVO implements Serializable {

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

    @Schema(description = "正文（富文本）")
    private String content;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "浏览量（含尚未回写数据库的 Redis 增量）")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "是否置顶 0否 1是")
    private Integer isTop;

    @Schema(description = "当前用户是否已点赞；未登录为 null")
    private Boolean liked;

    @Schema(description = "当前用户是否已收藏；未登录为 null")
    private Boolean favorited;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "相关推荐（同分类，不足时用热门补齐，最多 5 条）")
    private List<ArticleListVO> related;
}
