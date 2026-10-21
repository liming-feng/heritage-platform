package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单列表查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "订单列表查询条件")
public class OrderQueryDTO {

    @Schema(description = "订单状态：0待付款 1待发货 2待收货 3已完成 4已退款 5已取消；不传表示全部")
    private Integer status;

    @Schema(description = "订单号（模糊匹配）")
    private String orderNo;

    @Schema(description = "只看有退款申请的订单（匠人端用）")
    private Boolean refundOnly;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }
        if (pageSize > 50) {
            pageSize = 50L;
        }
        if (orderNo != null) {
            orderNo = orderNo.trim();
            if (orderNo.isEmpty()) {
                orderNo = null;
            }
        }
    }
}
