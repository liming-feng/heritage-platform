package com.lmf.heritage.module.craftsman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 匠人列表查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人列表查询条件")
public class CraftsmanQueryDTO {

    @Schema(description = "关键词，匹配姓名 / 技艺门类 / 简介")
    private String keyword;

    @Schema(description = "技艺门类，如 剪纸")
    private String craftType;

    @Schema(description = "排序：default综合 / follower关注数 / goods作品数 / new最新入驻")
    private String sort = "default";

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 12")
    private Long pageSize = 12L;

    /** 归一化分页与关键词，防越界 */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 12L;
        }
        if (pageSize > 100) {
            pageSize = 100L;
        }
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }
        if (sort == null || sort.isBlank()) {
            sort = "default";
        }
    }
}
