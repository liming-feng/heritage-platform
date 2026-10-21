package com.lmf.heritage.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 资讯批量发布 / 批量下架
 *
 * <p>与「批量审核」那类操作不同，发布/下架不存在「起始状态不合法」的问题
 * （下架一篇已经下架的资讯是幂等的），所以这里<b>允许重复执行</b>，
 * 返回的生效条数就是实际被 UPDATE 影响的行数。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "资讯批量发布/下架参数")
public class ArticleBatchStatusDTO {

    @NotEmpty(message = "请至少选择一篇资讯")
    @Size(max = 100, message = "单次最多操作 100 篇")
    @Schema(description = "资讯 id 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @NotNull(message = "请选择目标状态")
    @Schema(description = "目标状态 0草稿/下架 1已发布", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
