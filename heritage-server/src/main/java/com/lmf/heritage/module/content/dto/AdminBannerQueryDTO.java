package com.lmf.heritage.module.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端 · 轮播图查询条件
 *
 * <p>与用户端不同：这里<b>不按 status 过滤</b>，停用的图也要能在后台看见
 * —— 否则管理员停用一张图之后它就从管理列表里消失了，
 * 想再启用只能去数据库里改。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端轮播图查询条件")
public class AdminBannerQueryDTO {

    @Schema(description = "位置 1首页 2课程页；不传表示全部")
    private Integer position;

    @Schema(description = "状态 0停用 1启用；不传表示全部")
    private Integer status;

    @Schema(description = "关键词：匹配标题 / 跳转链接")
    private String keyword;

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
        if (pageSize > 200) {
            pageSize = 200L;
        }
        if (keyword != null) {
            String t = keyword.trim();
            keyword = t.isEmpty() ? null : t;
        }
    }
}
