package com.lmf.heritage.module.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 优惠券查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "优惠券查询条件")
public class CouponQueryDTO {

    @Schema(description = "我的优惠券状态筛选：0未使用 1已使用 2已过期，不传表示全部")
    private Integer status;

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
    }
}
