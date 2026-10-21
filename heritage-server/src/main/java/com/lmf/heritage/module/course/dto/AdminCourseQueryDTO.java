package com.lmf.heritage.module.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 · 全平台课程列表查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端课程列表查询条件")
public class AdminCourseQueryDTO {

    @Schema(description = "课程状态 0未开始 1进行中 2已结束")
    private Integer status;

    @Schema(description = "审核状态 0待审核 1已通过 2已驳回（含违规下架）")
    private Integer auditStatus;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "匠人 id（按匠人下钻）")
    private Long craftsmanId;

    @Schema(description = "关键词：匹配课程标题 / 副标题 / 匠人姓名")
    private String keyword;

    @Schema(description = "开课时间范围-起（含）")
    private LocalDateTime startTime;

    @Schema(description = "开课时间范围-止（含）")
    private LocalDateTime endTime;

    @Schema(description = "排序 new最新提交 / start开课时间 / hot报名热度 / quota名额紧张；默认 new")
    private String sort;

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
        if (sort == null || sort.isBlank()) {
            sort = "new";
        }
    }
}
