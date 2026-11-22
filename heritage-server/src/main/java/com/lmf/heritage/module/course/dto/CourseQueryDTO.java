package com.lmf.heritage.module.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 课程列表查询条件（前台）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "课程列表查询条件")
public class CourseQueryDTO {

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "匠人 id，用于「该匠人的课程」")
    private Long craftsmanId;

    @Schema(description = "关键词，匹配课程标题与副标题")
    private String keyword;

    @Schema(description = "课程状态 0未开始 1进行中 2已结束，不传表示全部")
    private Integer status;

    /**
     * 审核状态过滤，只对匠人端「我的课程」生效。
     *
     * <p>前台列表恒为 audit_status=1，传了也没有意义，SQL 里不会用到这个字段。</p>
     */
    @Schema(description = "审核状态 0待审核 1已通过 2已驳回，仅匠人端有效")
    private Integer auditStatus;

    @Schema(description = "是否只看还能预约的（未结束且未过预约截止）")
    private Boolean onlyBookable;

    @Schema(description = "排序：default综合 / price_asc / price_desc / new最新开课 / hot热门")
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
