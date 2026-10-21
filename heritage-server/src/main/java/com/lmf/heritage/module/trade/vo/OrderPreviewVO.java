package com.lmf.heritage.module.trade.vo;

import com.lmf.heritage.module.trade.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 结算预览（确认订单页）
 *
 * <p>只做「算账和校验」，不写库：把本次要下单的商品、金额、地址都算好给用户确认，
 * 用户点「提交订单」时才真正走创建流程。校验失败（库存不足 / 商品下架）
 * 在这一步就能提示，避免用户填完地址才发现买不了。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "结算预览")
public class OrderPreviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "默认收货地址，没有地址时为 null")
    private Address address;

    @Schema(description = "待下单商品项")
    private List<CartItemVO> items;

    @Schema(description = "商品总额")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额（本次使用的优惠券抵扣额；未用券时为 0.00）")
    private BigDecimal discountAmount;

    @Schema(description = "本次使用的用户券 id（未用券时为 null）；券的规则文案由「可用券」接口一并返回")
    private Long userCouponId;

    @Schema(description = "应付金额 = 商品总额 − 优惠金额")
    private BigDecimal payAmount;

    @Schema(description = "商品总件数")
    private Integer totalQuantity;

    @Schema(description = "是否全部可下单（存在失效商品时为 false）")
    private Boolean canSubmit;

    @Schema(description = "不可下单的原因提示")
    private String message;
}
