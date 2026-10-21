package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 匠人发货请求
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "订单发货请求")
public class OrderDeliverDTO {

    @NotNull(message = "订单 id 不能为空")
    @Schema(description = "订单 id")
    private Long orderId;

    @Size(max = 50, message = "物流公司最长 50 个字符")
    @Schema(description = "物流公司", example = "顺丰速运")
    private String logisticsCompany;

    @Size(max = 50, message = "物流单号最长 50 个字符")
    @Schema(description = "物流单号", example = "SF1234567890")
    private String logisticsNo;

    @Size(max = 255, message = "匠人备注最长 255 个字符")
    @Schema(description = "匠人备注", example = "已仔细包装，请注意查收")
    private String craftsmanRemark;
}
