package com.lmf.heritage.module.content.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资讯点赞
 *
 * <p><b>这张表没有逻辑删除字段</b>，取消点赞是物理删除。
 * 之所以和业务表的做法不一样，是因为它承担的是「去重」职责：
 * 唯一键 {@code uk_user_article(user_id, article_id)} 保证同一个人对同一篇资讯
 * 只可能有一条点赞记录，{@code article.like_count} 才敢放心地 +1 / -1。
 * 如果这里也走逻辑删除，唯一键会继续拦住第二次点赞，反而要在应用层处理「复活」，
 * 徒增复杂度（这一点与预约表不同——预约的取消是业务状态，点赞的取消就是没点过）。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("article_like")
@Schema(description = "资讯点赞")
public class ArticleLike implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "资讯 article.id")
    private Long articleId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
