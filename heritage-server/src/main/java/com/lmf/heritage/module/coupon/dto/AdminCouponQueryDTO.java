package com.lmf.heritage.module.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端 · 券种列表查询条件
 *
 * <p>比用户端领券中心多了 {@code status} 与 {@code effective} 两个维度：
 * 领券中心只该出现「启用且生效中」的券，而运营后台要能翻出已停用的、
 * 已过期的、以及还没到开始时间的券 —— 后者尤其重要，配错了开始时间
 * 「券不见了」是最常被误报成 bug 的情况。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端券种查询条件")
public class AdminCouponQueryDTO {

    @Schema(description = "关键词：匹配券名称")
    private String name;

    @Schema(description = "类型 1满减 2折扣；不传表示全部")
    private Integer type;

    @Schema(description = "适用范围 1全场通用 2仅商品 3仅课程；不传表示全部")
    private Integer scope;

    @Schema(description = "状态 0停用 1启用；不传表示全部")
    private Integer status;

    @Schema(description = "有效期状态 1未开始 2进行中 3已结束；不传表示全部")
    private Integer effective;

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
        if (name != null) {
            String t = name.trim();
            name = t.isEmpty() ? null : t;
        }
    }
}
