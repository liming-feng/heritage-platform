package com.lmf.heritage.module.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 预约列表查询条件（用户端「我的预约」与匠人端「预约管理」共用）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "预约列表查询条件")
public class BookingQueryDTO {

    @Schema(description = "状态筛选 0待审核 1已通过 2已拒绝 3已取消 4已核销 5已完成，不传表示全部")
    private Integer status;

    @Schema(description = "课程 id 筛选")
    private Long courseId;

    @Schema(description = "关键词：匹配预约单号 / 课程标题 / 联系人")
    private String keyword;

    @Schema(description = "匠人端专用：只看已超过审核时限仍未处理的预约")
    private Boolean onlyTimeout;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    /** 归一化分页参数，防越界 */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
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
    }
}
