package com.lmf.heritage.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 轮播图批量排序与启停
 *
 * <p>做成批量而不是逐个提交，理由与分类批量排序一致：轮播的顺序是一个整体，
 * 「改了一半」会留下前后矛盾的排列，而逐个请求中途失败时恰好就是这个结果。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "轮播图批量排序 / 启停参数")
public class BannerBatchSortDTO {

    @NotEmpty(message = "请至少提交一张轮播图")
    @Size(max = 100, message = "单次最多调整 100 张")
    @Schema(description = "待调整的轮播图列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Data
    @Schema(description = "单张轮播图的排序与状态")
    public static class Item {

        @NotNull(message = "轮播图 id 不能为空")
        @Schema(description = "轮播图 id", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long id;

        @Schema(description = "排序号，越小越前；不传表示不改")
        private Integer sort;

        @Schema(description = "状态 0停用 1启用；不传表示不改")
        private Integer status;
    }
}
