package com.lmf.heritage.module.goods.entity;

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
 * 手工艺品商品
 *
 * <p>前台上架条件：{@code status = 1（上架）}；status 的 2 表示「待审核」，
 * 由匠人发布后进入，管理端审核（audit_status）通过后才置为 1。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("goods")
@Schema(description = "手工艺品商品")
public class Goods implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属匠人 craftsman.id")
    private Long craftsmanId;

    @Schema(description = "分类 category.id")
    private Long categoryId;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题 / 卖点")
    private String subtitle;

    @Schema(description = "封面图 URL")
    private String cover;

    @Schema(description = "简要描述")
    private String description;

    @Schema(description = "详情（富文本）")
    private String detail;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价（划线价）")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "浏览量（Redis 计数器每小时回写）")
    private Integer viewCount;

    @Schema(description = "是否纯手工 0否 1是")
    private Integer isHandmade;

    @Schema(description = "状态 0下架 1上架 2待审核")
    private Integer status;

    @Schema(description = "审核状态 0待审核 1通过 2驳回")
    private Integer auditStatus;

    @Schema(description = "驳回原因")
    private String rejectReason;

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
