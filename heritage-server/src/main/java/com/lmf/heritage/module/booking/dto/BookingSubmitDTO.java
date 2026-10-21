package com.lmf.heritage.module.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 提交预约请求
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "提交预约请求")
public class BookingSubmitDTO {

    @NotNull(message = "请选择要预约的课程")
    @Schema(description = "课程 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long courseId;

    @NotBlank(message = "请填写联系人")
    @Schema(description = "联系人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactName;

    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactPhone;

    @Min(value = 1, message = "至少预约 1 个名额")
    @Max(value = 5, message = "单次最多预约 5 个名额")
    @Schema(description = "预约名额数（含陪同），默认 1")
    private Integer quota = 1;

    @Schema(description = "备注（如过敏史、特殊需求）")
    private String remark;
}
