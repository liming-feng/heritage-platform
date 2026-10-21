package com.lmf.heritage.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 留言反馈查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "留言反馈查询条件")
public class FeedbackQueryDTO {

    @Schema(description = "状态筛选 0待处理 1处理中 2已完成，不传表示全部")
    private Integer status;

    @Schema(description = "类型筛选 1功能建议 2内容纠错 3投诉举报 4其他，不传表示全部")
    private Integer type;

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
        if (pageSize > 50) {
            pageSize = 50L;
        }
    }
}
