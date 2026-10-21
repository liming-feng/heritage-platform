package com.lmf.heritage.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交留言反馈
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "提交留言反馈请求")
public class FeedbackDTO {

    @NotNull(message = "请选择反馈类型")
    @Min(value = 1, message = "反馈类型只能是 1功能建议 2内容纠错 3投诉举报 4其他")
    @Max(value = 4, message = "反馈类型只能是 1功能建议 2内容纠错 3投诉举报 4其他")
    @Schema(description = "类型 1功能建议 2内容纠错 3投诉举报 4其他", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;

    @NotBlank(message = "请填写反馈内容")
    @Size(max = 1000, message = "反馈内容最长 1000 个字符")
    @Schema(description = "反馈内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Size(max = 100, message = "联系方式最长 100 个字符")
    @Schema(description = "联系方式（手机号 / 邮箱，便于回复）")
    private String contact;
}
