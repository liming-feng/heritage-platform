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
import java.time.LocalDateTime;

/**
 * 购物车项
 *
 * <p>同一用户 + 同一商品 + 同一规格只保留一行，重复加购由业务层做数量累加。
 * 注意 {@code spec_id} 可为 null（无规格商品），而 MySQL 唯一索引对 null 不去重，
 * 所以「无规格」的判重必须在业务层用 {@code spec_id IS NULL} 显式处理。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("cart")
@Schema(description = "购物车项")
public class Cart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "商品 goods.id")
    private Long goodsId;

    @Schema(description = "规格 goods_spec.id，可为空")
    private Long specId;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "是否勾选 0否 1是")
    private Integer checked;

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
