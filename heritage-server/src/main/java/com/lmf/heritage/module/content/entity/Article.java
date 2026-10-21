package com.lmf.heritage.module.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 非遗资讯文章
 *
 * <p>P3 只用到「标题 / 封面 / 摘要」来补全浏览历史，资讯的增删改查与评论、点赞在 P6 展开。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("article")
@Schema(description = "非遗资讯")
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "分类 category.id")
    private Long categoryId;

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

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "是否置顶 0否 1是")
    private Integer isTop;

    @Schema(description = "状态 0草稿/下架 1已发布")
    private Integer status;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除 0未删 1已删")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
