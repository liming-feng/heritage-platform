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
 * 匠人扩展信息（与 user 一对一）
 *
 * <p>{@code idCardNo} / {@code certImage} 属于资质审核材料，加 {@code @JsonIgnore}
 * 避免前台接口把证件信息带出去。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("craftsman")
@Schema(description = "匠人")
public class Craftsman implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "关联 user.id")
    private Long userId;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "技艺门类，如 剪纸 / 木雕")
    private String craftType;

    @Schema(description = "师承")
    private String inheritance;

    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "履历（富文本）")
    private String resume;

    @Schema(description = "主页封面图")
    private String cover;

    /** 证件号：资质审核用，不对外返回 */
    @JsonIgnore
    @Schema(description = "证件号（资质审核用，不对外返回）")
    private String idCardNo;

    /** 资质证明图：资质审核用，不对外返回 */
    @JsonIgnore
    @Schema(description = "资质证明图（不对外返回）")
    private String certImage;

    @Schema(description = "审核状态 0待审核 1已通过 2已驳回")
    private Integer auditStatus;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "关注数")
    private Integer followerCount;

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
