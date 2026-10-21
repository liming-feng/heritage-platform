package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 购物车勾选 / 取消勾选请求
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "购物车勾选请求")
public class CartCheckDTO {

    @Schema(description = "购物车项 id 列表；不传表示全选/全不选")
    private List<Long> ids;

    @NotNull(message = "勾选状态不能为空")
    @Schema(description = "是否勾选 0否 1是")
    private Integer checked;
}
