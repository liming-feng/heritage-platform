package com.lmf.heritage.module.craftsman.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端 · 匠人列表项
 *
 * <p>列表里刻意<b>不带</b>证件号与资质图：一是审核列表一次几十条，没必要把大字段拖下来；
 * 二是资质材料属于敏感信息，只在「审核详情」这一条确定的链路上才返回。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端匠人列表项")
public class AdminCraftsmanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "匠人 id")
    private Long id;

    @Schema(description = "关联用户 id")
    private Long userId;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "技艺门类")
    private String craftType;

    @Schema(description = "师承")
    private String inheritance;

    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "主页封面图")
    private String cover;

    @Schema(description = "资质审核状态 0待审核 1已通过 2已驳回")
    private Integer auditStatus;

    @Schema(description = "审核状态文案")
    private String auditStatusText;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "账号状态 0禁用 1正常")
    private Integer status;

    @Schema(description = "账号状态文案")
    private String statusText;

    @Schema(description = "关注数")
    private Integer followerCount;

    @Schema(description = "在售作品数（已上架且已过审）")
    private Integer goodsCount;

    @Schema(description = "已开课数")
    private Integer courseCount;

    @Schema(description = "申请时间（即匠人档案创建时间）")
    private LocalDateTime createTime;
}
