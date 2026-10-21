package com.lmf.heritage.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端 · 资讯新增 / 编辑
 *
 * <p>{@code status} 允许为空：新增时没填就存草稿（0），
 * 让运营可以先把素材堆进来再决定发不发 —— 「保存即发布」会逼着人
 * 在还没校对完的时候就谨慎操作，或者干脆不敢用草稿功能。</p>
 *
 * <p>正文 {@code content} 不限制长度：富文本带 HTML 标签后体积膨胀很快，
 * 按字符数卡一个上限只会让编辑器在边界上莫名其妙地失败。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "资讯保存参数")
public class ArticleSaveDTO {

    @Schema(description = "资讯 id；不传为新增，传为编辑")
    private Long id;

    @NotBlank(message = "资讯标题不能为空")
    @Size(max = 150, message = "标题不能超过 150 字")
    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "分类 category.id，可为空（无分类归属）")
    private Long categoryId;

    @Size(max = 255, message = "封面图地址不能超过 255 字")
    @Schema(description = "封面图 URL")
    private String cover;

    @Size(max = 500, message = "摘要不能超过 500 字")
    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "正文（富文本 HTML）")
    private String content;

    @Size(max = 50, message = "作者不能超过 50 字")
    @Schema(description = "作者")
    private String author;

    @Size(max = 255, message = "标签不能超过 255 字")
    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "是否置顶 0否 1是；不传时新增默认否、编辑保持原值")
    private Integer isTop;

    @Schema(description = "状态 0草稿/下架 1已发布；不传时新增默认草稿、编辑保持原值")
    private Integer status;

    public void normalize() {
        if (title != null) {
            title = title.trim();
        }
        if (cover != null) {
            String t = cover.trim();
            cover = t.isEmpty() ? null : t;
        }
        if (summary != null) {
            String t = summary.trim();
            summary = t.isEmpty() ? null : t;
        }
        if (author != null) {
            String t = author.trim();
            author = t.isEmpty() ? null : t;
        }
        if (tags != null) {
            String t = tags.trim();
            tags = t.isEmpty() ? null : t;
        }
    }
}
