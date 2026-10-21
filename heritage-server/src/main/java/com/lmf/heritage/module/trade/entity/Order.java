package com.lmf.heritage.module.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 *
 * <p>状态流转见 {@code OrderStatusEnum}：
 * 待付款(0) → 待发货(1) → 待收货(2) → 已完成(3)；
 * 待付款可取消(5)；待发货/待收货可退款(4)。</p>
 *
 * <p><b>退款为什么单独用 refund_status：</b>订单状态里没有「退款中」这个值，
 * 若用 {@code refund_reason} 是否为空来判断「申请中」，驳回后又要清空该字段，
 * 语义会变得很脆弱。因此单独加一个 {@code refund_status}
 * （0无 1申请中 2已同意 3已驳回），订单状态保持"用户看到的履约状态"不变。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("orders")
@Schema(description = "订单")
public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "匠人 craftsman.id（便于匠人端分单）")
    private Long craftsmanId;

    @Schema(description = "商品总额")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "使用的优惠券 coupon.id")
    private Long couponId;

    @Schema(description = "用户券 user_coupon.id")
    private Long userCouponId;

    @Schema(description = "状态 0待付款 1待发货 2待收货 3已完成 4已退款 5已取消")
    private Integer status;

    @Schema(description = "收货地址快照（JSON）")
    private String addressSnapshot;

    @Schema(description = "收货人快照")
    private String receiver;

    @Schema(description = "收货电话快照")
    private String receiverPhone;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "物流单号")
    private String logisticsNo;

    @Schema(description = "买家备注")
    private String remark;

    @Schema(description = "匠人备注")
    private String craftsmanRemark;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "退款驳回原因")
    private String refundRejectReason;

    @Schema(description = "退款状态 0无 1申请中 2已同意 3已驳回")
    private Integer refundStatus;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "发货时间")
    private LocalDateTime deliveryTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除 0未删 1已删")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
