package com.lmf.heritage.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发表商品评价请求
 *
 * <p>评价资格由订单项驱动：必须传入自己已完成订单里的 {@code orderItemId}，
 * 后端据此校验归属与「是否已评价」，避免刷评价。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "发表商品评价请求")
public class ReviewPublishDTO {

    @NotNull(message = "订单项不能为空")
    @Schema(description = "订单项 order_item.id")
    private Long orderItemId;

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
