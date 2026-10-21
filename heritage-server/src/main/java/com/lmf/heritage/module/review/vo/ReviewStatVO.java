package com.lmf.heritage.module.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 匠人端评价统计
 *
 * <p>只统计「本匠人的商品与课程收到的评价」，口径与列表接口完全一致——
 * 两处用同一个 WHERE，避免出现「统计说 42 条、列表只翻得到 40 条」这类对不上的情况。</p>
 *
 * <p><b>好评 / 中评 / 差评的划分</b>：4-5 星为好评、3 星为中评、1-2 星为差评。
 * 好评率 = 好评数 / 总数，是电商里最常被引用的口碑指标。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人端评价统计")
public class ReviewStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评价总数")
    private Long total;

    @Schema(description = "平均分（1 位小数），无评价时为 0")
    private Double avgRating;

    @Schema(description = "好评数（4-5 星）")
    private Long goodCount;

    @Schema(description = "中评数（3 星）")
    private Long midCount;

    @Schema(description = "差评数（1-2 星）")
    private Long badCount;

    @Schema(description = "好评率（0-100，1 位小数）")
    private Double goodRate;

    @Schema(description = "带图评价数")
    private Long imageCount;

    @Schema(description = "已回复数")
    private Long repliedCount;

    @Schema(description = "未回复数（待办提醒用）")
    private Long unrepliedCount;

    @Schema(description = "回复率（0-100，1 位小数）")
    private Double repliedRate;

    @Schema(description = "各星级分布（5 星 → 1 星），可直接喂给图表")
    private List<StarDist> starDist;

    /**
     * 单个星级的分布
     *
     * <p>做成列表而不是 {@code star5/star4/...} 五个字段：前端画柱状图时可以直接
     * {@code v-for} 遍历，加星级也不用改前端。</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "星级分布项")
    public static class StarDist implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "星级 5-1")
        private Integer star;

        @Schema(description = "该星级评价数")
        private Long count;

        @Schema(description = "占比（0-100，1 位小数）")
        private Double percent;
    }
}
