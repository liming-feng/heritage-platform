package com.lmf.heritage.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 · 用户列表行
 *
 * <p>把匠人的资质字段一起带出来，是因为管理端「用户管理」页面实际上要同时服务三件事：
 * 看普通用户、看匠人、审匠人入驻。如果拆成两个页面，运营要在两个入口之间来回切；
 * 而这三件事看的是同一张 user 表，只是一个 LEFT JOIN 的差别。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端用户列表行")
public class AdminUserVO {

    @Schema(description = "用户 id")
    private Long id;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "手机号（管理端可见全量）")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "性别文案")
    private String genderText;

    @Schema(description = "角色 1用户 2匠人 3管理员")
    private Integer role;

    @Schema(description = "角色文案")
    private String roleText;

    @Schema(description = "状态 0禁用 1正常")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusText;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;

    @Schema(description = "最近更新时间")
    private LocalDateTime updateTime;

    /* ---------- 以下仅 role=2（匠人）时有值 ---------- */

    @Schema(description = "匠人 id（craftsman.id）")
    private Long craftsmanId;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "技艺门类")
    private String craftType;

    @Schema(description = "资质审核状态 0待审核 1已通过 2已驳回")
    private Integer auditStatus;

    @Schema(description = "资质审核状态文案")
    private String auditStatusText;

    @Schema(description = "驳回原因（audit_status=2 时有值）")
    private String rejectReason;

    @Schema(description = "关注数")
    private Integer followerCount;
}
