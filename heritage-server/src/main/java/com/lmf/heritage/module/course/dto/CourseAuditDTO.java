package com.lmf.heritage.module.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 课程审核（单件与批量共用一个结构）
 *
 * <p>单件审核时 {@code ids} 只放一个元素 —— 服务层是同一条路径，
 * 不为了「单件」再写一份只差个循环的实现。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "课程审核参数")
public class CourseAuditDTO {

    @NotEmpty(message = "请至少选择一门课程")
    @Size(max = 50, message = "单次最多审核 50 门")
    @Schema(description = "课程 id 列表，单次最多 50 个", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @NotNull(message = "审核结论不能为空")
    @Schema(description = "true 通过（前台可见并可预约）/ false 驳回（填原因）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean pass;

    @Schema(description = "驳回原因，pass=false 时必填")
    private String reason;
}
