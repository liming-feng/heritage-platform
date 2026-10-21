package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 订单备注请求（匠人端）
 *
 * <p>只改 {@code craftsman_remark}，与买家填的 {@code remark} 分开存：
 * 两边各自编辑自己的备注，谁也覆盖不了谁。这是订单里最容易出的问题之一
 * ——如果共用一个字段，匠人改个内部标记就会把买家的「请用红色包装」冲掉。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "订单备注请求")
public class OrderRemarkDTO {

    @NotNull(message = "订单 id 不能为空")
    @Schema(description = "订单 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @Size(max = 255, message = "备注最长 255 个字符")
    @Schema(description = "匠人备注；传空字符串表示清空备注")
    private String craftsmanRemark;
}
