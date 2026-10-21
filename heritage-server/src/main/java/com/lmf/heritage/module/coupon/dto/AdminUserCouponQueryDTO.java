package com.lmf.heritage.module.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 · 领取记录查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端领券记录查询条件")
public class AdminUserCouponQueryDTO {

    @Schema(description = "券种 id（在券种详情里下钻这一张券被谁领了）")
    private Long couponId;

    @Schema(description = "用户 id（按领取人下钻）")
    private Long userId;

    @Schema(description = "状态 0未使用 1已使用 2已过期；不传表示全部")
    private Integer status;

    @Schema(description = "关键词：匹配用户名 / 昵称 / 券名称 / 订单号")
    private String keyword;

    @Schema(description = "领取时间范围-起（含）")
    private LocalDateTime startTime;

    @Schema(description = "领取时间范围-止（含）")
    private LocalDateTime endTime;

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
        if (pageSize > 200) {
            pageSize = 200L;
        }
        if (keyword != null) {
            String t = keyword.trim();
            keyword = t.isEmpty() ? null : t;
        }
    }
}
