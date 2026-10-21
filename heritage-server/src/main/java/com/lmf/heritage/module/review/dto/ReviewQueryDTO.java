package com.lmf.heritage.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 匠人端评价列表查询条件
 *
 * <p>匠人关心的不是「某件商品的评价」，而是「我这摊子收到的评价」，
 * 所以这里的定位方式是 {@code craftsmanId} 而不是 {@code targetId}——
 * 由服务端把商品评价（type=1）与课程评价（type=2）两路合并后一起返回。
 * 换句话说，评价模块和预约模块不一样：它不是「选一门课再看它的评价」，
 * 而是一个统一的工作台。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人端评价列表查询条件")
public class ReviewQueryDTO {

    @Schema(description = "评价类型 1商品评价 2课程评价，不传表示两类都要")
    private Integer type;

    @Schema(description = "评价对象 id（某件商品或某门课程），不传表示全部")
    private Long targetId;

    @Schema(description = "按星级精确筛选 1-5，不传表示不限")
    private Integer rating;

    @Schema(description = "按口碑档位筛选：good 好评(4-5星) / mid 中评(3星) / bad 差评(1-2星)")
    private String ratingLevel;

    @Schema(description = "只看带图评价")
    private Boolean hasImage;

    @Schema(description = "回复状态：true 已回复 / false 未回复，不传表示不限")
    private Boolean replied;

    @Schema(description = "关键词，匹配评价内容 / 评价人昵称 / 商品或课程名称")
    private String keyword;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    /** 归一化分页与关键词，防越界 */
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
        if (ratingLevel != null && ratingLevel.isBlank()) {
            ratingLevel = null;
        }
    }
}
