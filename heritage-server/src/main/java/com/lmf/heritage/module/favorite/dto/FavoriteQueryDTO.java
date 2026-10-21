package com.lmf.heritage.module.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 我的收藏查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "我的收藏查询条件")
public class FavoriteQueryDTO {

    @Schema(description = "目标类型 1商品 2课程 3资讯，不传表示全部")
    private Integer targetType;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }
        if (pageSize > 50) {
            pageSize = 50L;
        }
    }
}
