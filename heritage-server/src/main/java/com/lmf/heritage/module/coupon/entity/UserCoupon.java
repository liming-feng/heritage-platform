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
import java.time.LocalDateTime;

/**
 * 用户优惠券（领取记录）
 *
 * <p>「领取」与「使用」两步都会改这张表：领取时插入一条 status=0，
 * 使用时把 status 改成 1 并写入 order_no。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("user_coupon")
@Schema(description = "用户优惠券")
public class UserCoupon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "优惠券 coupon.id")
    private Long couponId;

    @Schema(description = "状态 0未使用 1已使用 2已过期")
    private Integer status;

    @Schema(description = "使用的订单号")
    private String orderNo;

    @Schema(description = "使用时间")
    private LocalDateTime usedTime;

    @Schema(description = "过期时间（冗余自 coupon.end_time）")
    private LocalDateTime expireTime;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除 0未删 1已删")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "领取时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
