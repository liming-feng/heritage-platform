package com.lmf.heritage.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员处理留言反馈
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "处理留言反馈请求")
public class FeedbackHandleDTO {

    @NotNull(message = "请指定要处理的反馈")
    @Schema(description = "反馈 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "请选择目标状态")
    @Min(value = 0, message = "状态只能是 0待处理 1处理中 2已完成")
    @Max(value = 2, message = "状态只能是 0待处理 1处理中 2已完成")
    @Schema(description = "目标状态 0待处理 1处理中 2已完成", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Size(max = 1000, message = "回复最长 1000 个字符")
    @Schema(description = "管理员回复；流转到「已完成」时必填")
    private String reply;
}
