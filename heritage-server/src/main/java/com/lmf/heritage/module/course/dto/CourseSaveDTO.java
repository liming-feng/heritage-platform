package com.lmf.heritage.module.course.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程新增 / 编辑请求（匠人端）
 *
 * <p>{@code id} 为空表示新增，非空表示编辑。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "课程新增/编辑请求")
public class CourseSaveDTO {

    @Schema(description = "课程 id，不传表示新增")
    private Long id;

    @Schema(description = "非遗分类 id")
    private Long categoryId;

    @NotBlank(message = "请填写课程标题")
    @Schema(description = "课程标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "课程简介")
    private String description;

    @Schema(description = "课程详情（富文本）")
    private String detail;

    @Schema(description = "上课地点")
    private String address;

    @NotNull(message = "请选择开课时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开课时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @NotNull(message = "请选择结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;

    @DecimalMin(value = "0.00", message = "课程费用不能为负")
    @Schema(description = "课程费用")
    private BigDecimal price = BigDecimal.ZERO;

    @NotNull(message = "请填写总名额")
    @Min(value = 1, message = "总名额至少为 1")
    @Schema(description = "总名额", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalQuota;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预约截止时间，不传则默认取开课时间")
    private LocalDateTime bookingDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "可取消截止时间，不传则默认取开课时间")
    private LocalDateTime cancelDeadline;

    @Schema(description = "课时（分钟）")
    private Integer duration;

    @Schema(description = "难度 1入门 2进阶 3高级")
    private Integer difficulty = 1;
}
