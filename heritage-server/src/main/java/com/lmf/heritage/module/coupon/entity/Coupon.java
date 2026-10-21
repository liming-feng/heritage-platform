package com.lmf.heritage.module.coupon.entity;

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
 * 优惠券（券种定义）
 *
 * <p><b>这张表只描述「一种券长什么样」，不描述「谁领了」</b>——
 * 领取关系放在 {@code user_coupon} 里。两者分开的好处是：
 * 改一张券的说明文字不会碰到几十万条领取记录。</p>
 *
 * <p>{@code type} 是策略模式的派发依据：1 满减 → FullReductionStrategy，
 * 2 折扣 → DiscountStrategy。新增一种券型只需要加一个策略实现，
 * 这里与所有调用方都不用改。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("coupon")
@Schema(description = "优惠券")
public class Coupon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "类型 1满减 2折扣")
    private Integer type;

    @Schema(description = "使用门槛金额")
    private BigDecimal threshold;

    @Schema(description = "满减金额（type=1）")
    private BigDecimal amount;

    @Schema(description = "折扣率 0-1（type=2，如 0.85 表示 85 折）")
    private BigDecimal discount;

    @Schema(description = "发放总量")
    private Integer totalCount;

    @Schema(description = "已领取数量")
    private Integer receivedCount;

    @Schema(description = "每人限领张数")
    private Integer perLimit;

    @Schema(description = "适用范围 1全场通用 2仅商品 3仅课程")
    private Integer scope;

    @Schema(description = "有效期开始")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束")
    private LocalDateTime endTime;

    @Schema(description = "状态 0停用 1启用")
    private Integer status;

    @Schema(description = "使用说明")
    private String description;

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
