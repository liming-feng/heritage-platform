package com.lmf.heritage.module.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端 · 分类新增 / 编辑
 *
 * <p>{@code sort} 允许为空：新建分类时管理员通常还没想好排在哪，
 * 让服务层给一个「排到最后」的默认值，比强制他填一个数字更合理。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "分类保存参数")
public class CategorySaveDTO {

    @Schema(description = "分类 id；不传为新增，传为编辑")
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 字")
    @Schema(description = "分类名称（全平台唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 255, message = "图标地址不能超过 255 字")
    @Schema(description = "图标 URL")
    private String icon;

    @Size(max = 255, message = "分类描述不能超过 255 字")
    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序号，越小越前；不传时新增排在最后、编辑保持原值")
    private Integer sort;

    @Schema(description = "状态 0停用 1启用；不传时新增默认启用、编辑保持原值")
    private Integer status;

    public void normalize() {
        if (name != null) {
            name = name.trim();
        }
        if (icon != null) {
            String t = icon.trim();
            icon = t.isEmpty() ? null : t;
        }
        if (description != null) {
            String t = description.trim();
            description = t.isEmpty() ? null : t;
        }
    }
}
