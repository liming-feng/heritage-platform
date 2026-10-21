package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端 · 分类列表项
 *
 * <p>与前台 {@code CategoryVO} 的差别：<br>
 * ① 包含<b>停用</b>的分类 —— 停用的分类前台看不到，但管理端必须看得到，
 * 否则停用之后就再也点不回来了；<br>
 * ② 带 {@code sort} 与 {@code status} —— 这两个字段是分类管理的操作对象；<br>
 * ③ 统计口径扩到<b>全状态</b>商品与课程：{@code goodsCount} 用的是
 * 「上架且过审」（与前台角标一致），另给 {@code totalGoodsCount} 作删除前的判断依据 ——
 * 一件下架商品也是「分类下的商品」，删分类时不能装作没看见。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端分类列表项")
public class AdminCategoryVO implements Serializable {

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

    @Schema(description = "排序号，越小越前")
    private Integer sort;

    @Schema(description = "状态 0停用 1启用")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusText;

    @Schema(description = "在售商品数（上架且已过审，与前台角标同一口径）")
    private Integer goodsCount;

    @Schema(description = "商品总数（含下架 / 待审核，删除前的判断依据）")
    private Integer totalGoodsCount;

    @Schema(description = "课程数（全部状态）")
    private Integer courseCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
