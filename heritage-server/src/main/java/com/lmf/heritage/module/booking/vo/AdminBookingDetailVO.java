package com.lmf.heritage.module.booking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 管理端 · 预约详情（含完整状态流转轨迹）
 *
 * <p>这里用「组合」而不是继承 {@code BookingVO}：预约本身的字段有 40 多个，
 * 再抄一遍到管理端 VO 就意味着以后每加一个字段要改两处，迟早出现
 * 「列表有、详情没有」。组合则天然同步 —— 详情里读的就是列表那个对象。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端预约详情")
public class AdminBookingDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "预约主体信息（与列表同一份 VO）")
    private BookingVO booking;

    @Schema(description = "审核人名称（未审核时为 null）")
    private String auditorName;

    @Schema(description = "状态流转轨迹，按发生顺序")
    private List<BookingStatusLogVO> statusLogs;

    @Schema(description = "轨迹条数（没有轨迹说明是 P8 之前的历史数据）")
    private Integer trailCount;
}
