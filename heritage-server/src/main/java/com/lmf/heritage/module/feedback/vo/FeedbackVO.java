package com.lmf.heritage.module.feedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 留言反馈
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "留言反馈")
public class FeedbackVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "提交人 user.id")
    private Long userId;

    @Schema(description = "提交人用户名（管理端展示）")
    private String username;

    @Schema(description = "提交人昵称（管理端展示）")
    private String nickname;

    @Schema(description = "类型 1功能建议 2内容纠错 3投诉举报 4其他")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "管理员回复")
    private String reply;

    @Schema(description = "状态 0待处理 1处理中 2已完成")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "处理完成时间")
    private LocalDateTime handleTime;

    @Schema(description = "提交时间")
    private LocalDateTime createTime;
}
