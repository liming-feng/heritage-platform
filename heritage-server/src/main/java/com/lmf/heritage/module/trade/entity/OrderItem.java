package com.lmf.heritage.module.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项（商品快照）
 *
 * <p>下单那一刻把商品名、图片、规格名、成交单价全部**快照**进来：
 * 商品后续改价、改名、下架，历史订单展示的仍然是下单时的样子。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("order_item")
@Schema(description = "订单项（含商品快照）")
public class OrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "订单 orders.id")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商品 goods.id")
    private Long goodsId;

    @Schema(description = "规格 goods_spec.id")
    private Long specId;

    @Schema(description = "商品名称快照")
    private String goodsName;

    @Schema(description = "商品图片快照")
    private String goodsImage;

    @Schema(description = "规格名快照")
    private String specName;

    @Schema(description = "成交单价快照")
    private BigDecimal price;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "是否已评价 0否 1是")
    private Integer reviewed;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
