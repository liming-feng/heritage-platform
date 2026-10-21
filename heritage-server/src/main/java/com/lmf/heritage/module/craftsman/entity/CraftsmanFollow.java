package com.lmf.heritage.module.craftsman.entity;

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
 * 匠人关注关系
 *
 * <p>规划书里「匠人关注」属 T2 可裁剪项，但它是匠人主页的核心互动动作，
 * 且成本很低（一张关系表 + 一个计数），因此按计划实现。</p>
 *
 * <p>唯一键 {@code uk_user_craftsman(user_id, craftsman_id)} 保证关系唯一，
 * 取消关注走逻辑删除；再次关注时走「复活」而不是重新插入，避免撞唯一键。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("craftsman_follow")
@Schema(description = "匠人关注关系")
public class CraftsmanFollow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "匠人 craftsman.id")
    private Long craftsmanId;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除 0关注中 1已取消")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "关注时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
