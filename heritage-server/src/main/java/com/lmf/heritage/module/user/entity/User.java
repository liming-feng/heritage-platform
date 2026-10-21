package com.lmf.heritage.module.user.entity;

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
 * 用户实体（对应表 user，含普通用户 / 匠人 / 管理员三种角色）
 *
 * <p>表名加反引号：user 在 MySQL 中是关键字，加反引号可避免方言差异带来的 SQL 语法问题。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("`user`")
@Schema(description = "用户（含三种角色）")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "登录用户名")
    private String username;

    /**
     * BCrypt 密文。加 @JsonIgnore 保证任何接口都不会把密码返回给前端。
     */
    @JsonIgnore
    @Schema(description = "密码（BCrypt 密文，不对外返回）")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "角色 1普通用户 2匠人 3管理员")
    private Integer role;

    @Schema(description = "状态 0禁用 1正常")
    private Integer status;

    /** 逻辑删除，查询时 MyBatis-Plus 自动追加 deleted = 0 */
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
