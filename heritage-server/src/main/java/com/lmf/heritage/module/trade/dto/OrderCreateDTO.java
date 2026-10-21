package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求
 *
 * <p>两种下单入口共用这一个对象：</p>
 * <ul>
 *   <li><b>购物车结算</b>：{@code items} 不传，后端取当前用户购物车中**已勾选**的项；</li>
 *   <li><b>立即购买</b>：{@code items} 传具体商品与数量。</li>
 * </ul>
 * 下单成功后，来自购物车的那部分会被清出购物车。
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "创建订单请求")
public class OrderCreateDTO {

    @NotNull(message = "请先选择收货地址")
    @Schema(description = "收货地址 id")
    private Long addressId;

    @Size(max = 255, message = "备注最长 255 个字符")
    @Schema(description = "买家备注")
    private String remark;

    @Schema(description = "使用的用户优惠券 id（user_coupon.id，不是 coupon.id）；不传表示不使用优惠券")
    private Long userCouponId;

    @Schema(description = "直接购买的商品列表；不传表示结算购物车勾选项")
    private List<Item> items;

    /**
     * 直接购买时的单项
     */
    @Data
    @Schema(description = "下单商品项")
    public static class Item {

        @NotNull(message = "商品不能为空")
        @Schema(description = "商品 id")
        private Long goodsId;

        @Schema(description = "规格 id")
        private Long specId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 999, message = "单个商品最多购买 999 件")
        @Schema(description = "数量")
        private Integer quantity;
    }
}
