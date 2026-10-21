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
 * 资讯评论
 *
 * <p>只支持<b>一级回复</b>：{@code parent_id = 0} 是主楼，非 0 表示「回复某个主楼」。
 * 不做无限嵌套是有意的——两级结构用「主楼 + 回复列表」就能展示清楚，
 * 而无限嵌套需要递归建树、按需加载、楼层定位，收益远小于复杂度。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("article_comment")
@Schema(description = "资讯评论")
public class ArticleComment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "资讯 article.id")
    private Long articleId;

    @Schema(description = "评论人 user.id")
    private Long userId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "父评论 id，一级评论为 0")
    private Long parentId;

    @Schema(description = "点赞数")
    private Integer likeCount;

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
