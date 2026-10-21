package com.lmf.heritage.module.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯评论
 *
 * <p>主楼会带上自己的回复列表（{@code replies}），因此前端只需要一次请求
 * 就能渲染出「主楼 + 若干条回复」的结构。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "资讯评论")
public class ArticleCommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评论 id")
    private Long id;

    @Schema(description = "资讯 id")
    private Long articleId;

    @Schema(description = "评论人 user.id")
    private Long userId;

    @Schema(description = "评论人昵称")
    private String nickname;

    @Schema(description = "评论人头像")
    private String avatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "父评论 id，0 表示主楼")
    private Long parentId;

    @Schema(description = "被回复人的昵称（仅回复有此字段）")
    private String replyToNickname;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;

    @Schema(description = "回复列表（仅主楼返回）")
    private List<ArticleCommentVO> replies;
}
