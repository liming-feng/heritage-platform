package com.lmf.heritage.module.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改预约规则配置请求（管理端）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "修改预约规则配置")
public class BookingConfigUpdateDTO {

    @NotBlank(message = "请指定配置键")
    @Schema(description = "配置键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configKey;

    @NotBlank(message = "请填写配置值")
    @Schema(description = "配置值（以字符串提交，服务端按语义校验并转换）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String configValue;
}
