package com.lmf.heritage.module.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 · 券种列表项
 *
 * <p>比用户端 {@code CouponVO} 多了三个运营视角的字段：</p>
 * <ul>
 *   <li>{@code usedCount} 已使用张数 —— 券的真正价值看这个，不是看发出去多少；</li>
 *   <li>{@code useRate} 核销率 = 已使用 / 已领取，用来判断这张券是不是「发了个寂寞」；</li>
 *   <li>{@code effectiveDesc} 未开始 / 进行中 / 已结束，解释「为什么用户看不到这张券」。</li>
 * </ul>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端券种")
public class AdminCouponVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "券 id")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "类型 1满减 2折扣")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "适用范围 1全场通用 2仅商品 3仅课程")
    private Integer scope;

    @Schema(description = "适用范围描述")
    private String scopeDesc;

    @Schema(description = "使用门槛金额")
    private BigDecimal threshold;

    @Schema(description = "满减金额（type=1）")
    private BigDecimal amount;

    @Schema(description = "折扣率 0-1（type=2）")
    private BigDecimal discount;

    @Schema(description = "优惠规则文案，如「满 200 减 30 元」")
    private String ruleText;

    @Schema(description = "发放总量")
    private Integer totalCount;

    @Schema(description = "已领取数量（coupon 表计数）")
    private Integer receivedCount;

    @Schema(description = "剩余可领数量")
    private Integer remainCount;

    @Schema(description = "实际领取记录数（user_coupon 表计数，可与 receivedCount 对账）")
    private Integer receiveRecordCount;

    @Schema(description = "已使用张数")
    private Integer usedCount;

    @Schema(description = "领取率 %（已领取 / 发放总量）")
    private BigDecimal receiveRate;

    @Schema(description = "核销率 %（已使用 / 已领取）")
    private BigDecimal useRate;

    @Schema(description = "每人限领张数")
    private Integer perLimit;

    @Schema(description = "有效期开始")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束")
    private LocalDateTime endTime;

    @Schema(description = "券状态 0停用 1启用")
    private Integer status;

    @Schema(description = "券状态描述")
    private String statusDesc;

    @Schema(description = "有效期状态描述：未开始 / 进行中 / 已结束 / 长期有效")
    private String effectiveDesc;

    @Schema(description = "使用说明")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
