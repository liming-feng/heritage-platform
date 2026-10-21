package com.lmf.heritage.module.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 结算页可用券查询结果
 *
 * <p>除了券列表本身，还直接给出「最优抵扣」与「抵扣后金额」——
 * 结算页默认要展示「已为你省 X 元」，让前端再遍历一遍算最优解属于重复劳动，
 * 而且前后端各算一次很容易在舍入上对不齐。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "结算页可用券查询结果")
public class CouponAvailableResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单原始金额")
    private BigDecimal amount;

    @Schema(description = "可用券数量（门槛已达成的）")
    private Integer availableCount;

    @Schema(description = "最优抵扣金额（没有可用券时为 0.00）")
    private BigDecimal bestDiscountAmount;

    @Schema(description = "最优抵扣后的应付金额")
    private BigDecimal bestPayAmount;

    @Schema(description = "命中的最优券的用户券 id（没有可用券时为 null）")
    private Long bestUserCouponId;

    @Schema(description = "可用券列表，按抵扣金额从高到低排序")
    private List<CouponAvailableVO> list;
}
