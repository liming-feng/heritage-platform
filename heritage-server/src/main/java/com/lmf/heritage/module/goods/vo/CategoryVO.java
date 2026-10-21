package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分类展示对象
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "非遗分类")
public class CategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分类 id")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "图标 URL")
    private String icon;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "该分类下的上架商品数")
    private Integer goodsCount;
}
