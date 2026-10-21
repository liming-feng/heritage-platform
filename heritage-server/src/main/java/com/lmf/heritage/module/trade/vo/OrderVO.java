package com.lmf.heritage.module.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图（列表与详情共用）
 *
 * <p>末尾那组 {@code canXxx} 是给前端用的按钮开关：前端不需要自己判断状态，
 * 直接按后端给的布尔值渲染按钮，避免前后端状态判断逻辑不一致。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "订单")
public class OrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单 id")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "状态 0待付款 1待发货 2待收货 3已完成 4已退款 5已取消")
    private Integer status;

    @Schema(description = "状态文字")
    private String statusDesc;

    @Schema(description = "商品总额")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "商品总件数")
    private Integer totalQuantity;

    @Schema(description = "收货人快照")
    private String receiver;

    @Schema(description = "收货电话快照")
    private String receiverPhone;

    @Schema(description = "收货地址快照")
    private String addressSnapshot;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "物流单号")
    private String logisticsNo;

    @Schema(description = "买家备注")
    private String remark;

    @Schema(description = "匠人备注")
    private String craftsmanRemark;

    @Schema(description = "退款状态 0无 1申请中 2已同意 3已驳回")
    private Integer refundStatus;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "退款驳回原因")
    private String refundRejectReason;

    @Schema(description = "买家昵称（匠人端 / 管理端展示）")
    private String buyerName;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "发货时间")
    private LocalDateTime deliveryTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "订单项列表")
    private List<OrderItemVO> items;

    /* ---------- 前端按钮开关 ---------- */

    @Schema(description = "是否可支付")
    private Boolean canPay;

    @Schema(description = "是否可取消")
    private Boolean canCancel;

    @Schema(description = "是否可申请退款")
    private Boolean canRefund;

    @Schema(description = "是否可确认收货")
    private Boolean canConfirm;

    @Schema(description = "是否可评价")
    private Boolean canReview;

    @Schema(description = "是否可发货（匠人端）")
    private Boolean canDeliver;

    @Schema(description = "是否有待处理的退款申请（匠人端）")
    private Boolean canAuditRefund;
}
