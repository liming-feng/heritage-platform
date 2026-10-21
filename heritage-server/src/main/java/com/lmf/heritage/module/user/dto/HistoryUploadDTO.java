package com.lmf.heritage.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 浏览历史上报（未登录期间存在 localStorage 的记录，登录后一次性上传）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "浏览历史上报")
public class HistoryUploadDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "上报内容不能为空")
    @Valid
    @Schema(description = "浏览记录列表，最多 50 条")
    private List<Item> items;

    @Data
    @Schema(description = "单条浏览记录")
    public static class Item implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotNull(message = "targetType 不能为空")
        @Schema(description = "1商品 2课程 3资讯")
        private Integer targetType;

        @NotNull(message = "targetId 不能为空")
        @Schema(description = "目标 id")
        private Long targetId;

        @Schema(description = "浏览时间（前端记录的原始时间），不传则用当前时间")
        private LocalDateTime browseTime;
    }
}
