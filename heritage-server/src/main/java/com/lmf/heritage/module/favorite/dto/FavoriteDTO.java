package com.lmf.heritage.module.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏 / 取消收藏请求
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "收藏操作请求")
public class FavoriteDTO {

    @NotNull(message = "请指定收藏类型")
    @Min(value = 1, message = "收藏类型只能是 1商品 2课程 3资讯")
    @Max(value = 3, message = "收藏类型只能是 1商品 2课程 3资讯")
    @Schema(description = "目标类型 1商品 2课程 3资讯", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer targetType;

    @NotNull(message = "请指定收藏目标")
    @Schema(description = "目标 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetId;
}
