package com.lmf.heritage.module.favorite.entity;

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
 * 收藏（商品 / 课程 / 资讯共用一张表）
 *
 * <p>用 {@code target_type + target_id} 表达「收藏了什么」，而不是给商品、课程、资讯
 * 各建一张收藏表。这样「我的收藏」一个接口就能查全部，前端也不用调三次。</p>
 *
 * <p><b>关于唯一键与逻辑删除的冲突：</b>表上有
 * {@code uk_user_target(user_id, target_type, target_id)}，而本表又走逻辑删除。
 * 于是「取消收藏后再收藏」会撞唯一键——已删除的行仍然占着那个键。
 * 因此新增收藏不能直接 insert，必须用
 * {@code INSERT ... ON DUPLICATE KEY UPDATE deleted = 0}（见 FavoriteMapper），
 * 让这条老记录「复活」。这一点与预约表上的 uk_user_course 是同类问题，
 * 处理手法也一致。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("favorite")
@Schema(description = "收藏")
public class Favorite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "目标类型 1商品 2课程 3资讯")
    private Integer targetType;

    @Schema(description = "目标 id")
    private Long targetId;

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
