package com.lmf.heritage.module.feedback.entity;

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
 * 留言反馈
 *
 * <p>{@code user_id} 允许为空 —— 未登录用户也能提意见（比如登录不上来才要反馈），
 * 因此「回复通知」只在有 userId 时才发。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("feedback")
@Schema(description = "留言反馈")
public class Feedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "提交人 user.id（未登录时为空）")
    private Long userId;

    @Schema(description = "类型 1功能建议 2内容纠错 3投诉举报 4其他")
    private Integer type;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "管理员回复")
    private String reply;

    @Schema(description = "状态 0待处理 1处理中 2已完成")
    private Integer status;

    @Schema(description = "处理完成时间")
    private LocalDateTime handleTime;

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
