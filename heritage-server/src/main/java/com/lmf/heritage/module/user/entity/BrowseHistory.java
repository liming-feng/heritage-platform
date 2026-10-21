package com.lmf.heritage.module.user.entity;

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
 * 浏览历史（登录用户的服务端记录）
 *
 * <p>表上有唯一键 {@code uk_user_target(user_id, target_type, target_id)}，
 * 所以「重复浏览」走 update 更新时间，而不是插新行 —— 这正是需求里「上报去重」的数据库级保证。</p>
 *
 * <p>未登录时前端存 localStorage（限 50 条），登录后一次性上报，由 upload 接口批量写入。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("browse_history")
@Schema(description = "浏览历史")
public class BrowseHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "1商品 2课程 3资讯")
    private Integer targetType;

    @Schema(description = "目标 id")
    private Long targetId;

    /** 浏览时间由业务显式赋值（离线记录上传时要用前端记录的原始时间，不能用入库时间） */
    @Schema(description = "浏览时间")
    private LocalDateTime browseTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
