package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 商品批量上下架请求
 *
 * <p>一次最多 50 件：批量接口没有逐条确认的机会，单次范围过大时一旦参数给错
 * 影响面难以控制，所以在入口就设一道上限。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品批量上下架请求")
public class GoodsShelfDTO {

    @NotEmpty(message = "请至少选择一件商品")
    @Size(max = 50, message = "单次最多操作 50 件商品")
    @Schema(description = "商品 id 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @NotNull(message = "请指定上架或下架")
    @Schema(description = "true 上架 / false 下架", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean onShelf;
}
