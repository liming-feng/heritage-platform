package com.lmf.heritage.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 浏览历史条目（已补全标题 / 封面 / 价格，前端不用再逐条回查）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "浏览历史条目")
public class HistoryItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "1商品 2课程 3资讯")
    private Integer targetType;

    @Schema(description = "目标 id")
    private Long targetId;

    @Schema(description = "标题（商品名 / 课程名 / 资讯标题）")
    private String title;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "价格（资讯为 null）")
    private BigDecimal price;

    @Schema(description = "副标题 / 摘要")
    private String subtitle;

    @Schema(description = "浏览时间")
    private LocalDateTime browseTime;
}
