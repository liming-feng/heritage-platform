package com.lmf.heritage.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发表课程评价请求
 *
 * <p>与商品评价同一套思路：评价资格由「预约记录」驱动，前端只传 {@code bookingId}，
 * 后端据此反查课程、用户与状态，避免改个参数就能给没上过的课打分。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "发表课程评价请求")
public class ReviewPublishCourseDTO {

    @NotNull(message = "预约记录不能为空")
    @Schema(description = "预约记录 booking.id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookingId;

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    @Schema(description = "评分 1-5", example = "5")
    private Integer rating;

    @Size(max = 1000, message = "评价内容最长 1000 个字符")
    @Schema(description = "评价内容")
    private String content;

    @Size(max = 3, message = "最多上传 3 张图片")
    @Schema(description = "图片 URL 列表，最多 3 张")
    private List<String> images;
}
