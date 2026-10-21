package com.lmf.heritage.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论 / 回复
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "发表评论请求")
public class ArticleCommentDTO {

    @NotNull(message = "请指定要评论的资讯")
    @Schema(description = "资讯 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long articleId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论最长 1000 个字符")
    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "父评论 id；0 或不传表示发表主楼评论，非 0 表示回复该主楼")
    private Long parentId = 0L;
}
