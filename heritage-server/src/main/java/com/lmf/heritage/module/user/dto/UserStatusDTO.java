package com.lmf.heritage.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端 · 启用/禁用用户
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "启用/禁用用户")
public class UserStatusDTO {

    @NotNull(message = "请指定用户")
    @Schema(description = "用户 id")
    private Long id;

    @NotNull(message = "请指定目标状态")
    @Min(value = 0, message = "状态只能是 0 禁用 / 1 正常")
    @Max(value = 1, message = "状态只能是 0 禁用 / 1 正常")
    @Schema(description = "0 禁用 / 1 正常")
    private Integer status;
}
