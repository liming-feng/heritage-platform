package com.lmf.heritage.module.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 · 创建 / 编辑优惠券
 *
 * <p><b>type 决定哪些字段有意义</b>：满减券看 {@code amount}，折扣券看 {@code discount}。
 * 服务层会在保存时清掉不属于当前类型的那一个，避免出现「折扣券上还挂着一个满减金额」
 * 这种让后续所有计算都要先判断用哪个字段的状态。</p>
 *
 * <p>{@code totalCount} 一旦有人领取就<b>不允许往下调</b>：
 * 调小会让「已领取 > 发放总量」，剩余量算成负数，领券中心的按钮状态全部失真。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "优惠券保存参数")
public class CouponSaveDTO {

    @Schema(description = "券 id；不传为新增，传为编辑")
    private Long id;

    @NotBlank(message = "请填写优惠券名称")
    @Size(max = 100, message = "优惠券名称不能超过 100 字")
    @Schema(description = "优惠券名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "请选择优惠券类型")
    @Schema(description = "类型 1满减 2折扣", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;

    @Schema(description = "使用门槛金额，0 表示无门槛；不传按 0 处理")
    private BigDecimal threshold;

    @Schema(description = "满减金额（type=1 时必填）")
    private BigDecimal amount;

    @Schema(description = "折扣率 0-1（type=2 时必填，如 0.85 表示 85 折）")
    private BigDecimal discount;

    @NotNull(message = "请填写发放总量")
    @Schema(description = "发放总量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalCount;

    @Schema(description = "每人限领张数，默认 1")
    private Integer perLimit;

    @Schema(description = "适用范围 1全场通用 2仅商品 3仅课程，默认全场通用")
    private Integer scope;

    @Schema(description = "有效期开始；不传表示立即生效")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束；不传表示长期有效")
    private LocalDateTime endTime;

    @Schema(description = "状态 0停用 1启用；不传时新增默认启用、编辑保持原值")
    private Integer status;

    @Size(max = 255, message = "使用说明不能超过 255 字")
    @Schema(description = "使用说明")
    private String description;

    public void normalize() {
        if (name != null) {
            name = name.trim();
        }
        if (description != null) {
            String t = description.trim();
            description = t.isEmpty() ? null : t;
        }
    }
}
