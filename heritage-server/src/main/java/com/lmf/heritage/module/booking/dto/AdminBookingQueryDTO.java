package com.lmf.heritage.module.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 · 全平台预约列表查询条件
 *
 * <p>与用户端 / 匠人端共用的 {@code BookingQueryDTO} 分开：管理端多了
 * {@code userId}、{@code craftsmanId}、时间范围三类下钻维度，
 * 而用户端那两个字段是由登录身份决定的（不可能也不能让前端传）。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端预约列表查询条件")
public class AdminBookingQueryDTO {

    @Schema(description = "状态筛选 0待审核 1已通过 2已拒绝 3已取消 4已核销 5已完成")
    private Integer status;

    @Schema(description = "课程 id")
    private Long courseId;

    @Schema(description = "用户 id（按预约人下钻）")
    private Long userId;

    @Schema(description = "匠人 id（按匠人下钻）")
    private Long craftsmanId;

    @Schema(description = "关键词：匹配预约单号 / 课程标题 / 联系人 / 联系电话 / 匠人姓名")
    private String keyword;

    @Schema(description = "提交时间范围-起（含）")
    private LocalDateTime startTime;

    @Schema(description = "提交时间范围-止（含）")
    private LocalDateTime endTime;

    @Schema(description = "只看已超过审核时限仍未处理的待审核预约")
    private Boolean onlyTimeout;

    @Schema(description = "排序 new最新提交(默认) / old最早提交 / course开课时间")
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
