package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品规格展示对象
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品规格")
public class GoodsSpecVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "规格 id")
    private Long id;

    @Schema(description = "规格名，如 20cm / 30cm")
    private String specName;

    @Schema(description = "规格售价")
    private BigDecimal price;

    @Schema(description = "规格库存")
    private Integer stock;
}
