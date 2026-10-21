package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 分类批量调整排序与状态
 *
 * <p>分类管理页的典型操作是「拖一排顺序、顺手停用两个」。做成批量接口而不是
 * 逐个 PUT，是因为逐个提交在中途失败时会留下一个半新半旧的排序 ——
 * 而排序这种东西「改了一半」比「没改」更难收拾。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "分类批量排序 / 启停参数")
public class CategoryBatchSortDTO {

    @NotEmpty(message = "请至少提交一个分类")
    @Size(max = 100, message = "单次最多调整 100 个分类")
    @Schema(description = "待调整的分类列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Data
    @Schema(description = "单个分类的排序与状态")
    public static class Item {

        @NotNull(message = "分类 id 不能为空")
        @Schema(description = "分类 id", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long id;

        @Schema(description = "排序号，越小越前；不传表示不改")
        private Integer sort;

        @Schema(description = "状态 0停用 1启用；不传表示不改")
        private Integer status;
    }
}
