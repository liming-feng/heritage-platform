package com.lmf.heritage.module.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * 核销签到请求（匠人端）
 *
 * <p><b>两种核销方式，二选一</b>：<br>
 * ① {@code checkinCode} —— 用户出示核销码，匠人扫码或手工输入。线下签到台的主路径；<br>
 * ② {@code bookingId} —— 用户到场后匠人在报名名单里直接点「核销」。<br>
 * 两种方式的入口不同，但最终落到同一段状态流转（已通过 → 已核销），校验规则只有一份。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "核销签到请求（核销码与预约 id 二选一）")
public class BookingCheckinDTO {

    @Schema(description = "用户出示的核销码（扫码或手工输入时传）")
    private String checkinCode;

    @Schema(description = "预约 id（在报名名单里直接点核销时传）")
    private Long bookingId;

    /**
     * 二选一校验
     *
     * <p>不放在字段上用 {@code @NotBlank}：那样会把「手动核销」这条合法路径一并判为非法。
     * 两个字段各自都不是必填，但至少要有一个，只能在整个对象上校验。</p>
     */
    @JsonIgnore
    @AssertTrue(message = "请提供核销码或预约 id")
    public boolean isTargetProvided() {
        boolean hasCode = checkinCode != null && !checkinCode.isBlank();
        return hasCode || bookingId != null;
    }
}
