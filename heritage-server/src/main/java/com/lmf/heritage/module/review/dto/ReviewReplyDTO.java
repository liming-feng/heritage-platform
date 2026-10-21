package com.lmf.heritage.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 匠人回复评价请求
 *
 * <p>同一个接口既用于「首次回复」也用于「修改回复」：把回复当成一条可编辑的对外发言，
 * 而不是一次性的动作，匠人才可能在写错字之后改回来。前后端都不需要区分两种情况。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人回复评价请求")
public class ReviewReplyDTO {

    @NotNull(message = "评价 id 不能为空")
    @Schema(description = "评价 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long reviewId;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容最长 500 个字符")
    @Schema(description = "回复内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reply;
}
