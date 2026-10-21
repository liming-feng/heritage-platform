package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 退款相关请求（申请 / 审核）
 *
 * @author 李明峰 23100025
 */
public class RefundDTO {

    /**
     * 用户申请退款
     */
    @Data
    @Schema(description = "申请退款请求")
    public static class Apply {

        @NotNull(message = "订单 id 不能为空")
        @Schema(description = "订单 id")
        private Long orderId;

        @NotBlank(message = "请填写退款原因")
        @Size(max = 255, message = "退款原因最长 255 个字符")
        @Schema(description = "退款原因", example = "收到时器皿有磕碰")
        private String reason;
    }

    /**
     * 匠人 / 管理员审核退款
     */
    @Data
    @Schema(description = "退款审核请求")
    public static class Audit {

        @NotNull(message = "订单 id 不能为空")
        @Schema(description = "订单 id")
        private Long orderId;

        @NotNull(message = "请选择是否同意退款")
        @Schema(description = "是否同意 true同意 false驳回")
        private Boolean approve;

        @Size(max = 255, message = "驳回原因最长 255 个字符")
        @Schema(description = "驳回原因，审核驳回时必填")
        private String rejectReason;
    }
}
