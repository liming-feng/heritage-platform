package com.lmf.heritage.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 批量启用/禁用用户
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "批量启用/禁用用户")
public class UserBatchStatusDTO {

    @NotEmpty(message = "请至少选择一个用户")
    @Size(max = 50, message = "单次最多操作 50 个账号")
    @Schema(description = "用户 id 列表，单次上限 50")
    private List<Long> ids;

    @NotNull(message = "请指定目标状态")
    @Min(value = 0, message = "状态只能是 0 禁用 / 1 正常")
    @Max(value = 1, message = "状态只能是 0 禁用 / 1 正常")
    @Schema(description = "0 禁用 / 1 正常")
    private Integer status;
}
