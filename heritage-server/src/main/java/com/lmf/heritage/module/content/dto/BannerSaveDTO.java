package com.lmf.heritage.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端 · 轮播图新增 / 编辑
 *
 * <p>{@code image} 必填：轮播图没有图片就没有任何意义，与其让它在前端
 * 渲染成一块空白、再由用户来猜发生了什么，不如在入口就拦掉。</p>
 *
 * <p>{@code sort} 不传时新增默认排到最后（当前最大 sort + 1），
 * 编辑时保持原值 —— 与分类管理保持一致的处理方式。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "轮播图保存参数")
public class BannerSaveDTO {

    @Schema(description = "轮播 id；不传为新增，传为编辑")
    private Long id;

    @Size(max = 100, message = "标题不能超过 100 字")
    @Schema(description = "标题")
    private String title;

    @NotBlank(message = "请上传轮播图片")
    @Size(max = 255, message = "图片地址不能超过 255 字")
    @Schema(description = "图片 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String image;

    @Size(max = 255, message = "跳转链接不能超过 255 字")
    @Schema(description = "跳转链接")
    private String linkUrl;

    @Schema(description = "位置 1首页 2课程页；不传时新增默认首页、编辑保持原值")
    private Integer position;

    @Schema(description = "排序号，越小越前；不传时新增排到最后、编辑保持原值")
    private Integer sort;

    @Schema(description = "状态 0停用 1启用；不传时新增默认启用、编辑保持原值")
    private Integer status;

    public void normalize() {
        if (title != null) {
            String t = title.trim();
            title = t.isEmpty() ? null : t;
        }
        if (image != null) {
            image = image.trim();
        }
        if (linkUrl != null) {
            String t = linkUrl.trim();
            linkUrl = t.isEmpty() ? null : t;
        }
    }
}
