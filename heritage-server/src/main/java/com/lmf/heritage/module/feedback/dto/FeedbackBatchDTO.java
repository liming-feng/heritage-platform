package com.lmf.heritage.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 批量处理反馈
 *
 * <p>典型场景是「节后一次性清理掉一批同类建议」。做成批量而不是逐条点，
 * 是因为逐条处理时中途失败会留下一半已回复一半未回复的状态，
 * 而用户收到的通知是「已处理完成」—— 那时再想补就晚了。这里<b>整批回滚</b>。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "批量处理反馈参数")
public class FeedbackBatchDTO {

    @NotEmpty(message = "请至少选择一条反馈")
    @Size(max = 100, message = "单次最多处理 100 条")
    @Schema(description = "反馈 id 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @NotNull(message = "请选择目标状态")
    @Schema(description = "目标状态 0待处理 1处理中 2已完成", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Size(max = 1000, message = "回复最长 1000 个字符")
    @Schema(description = "统一回复内容；流转到「已完成」时必填")
    private String reply;
}
