package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 商品浏览量趋势（匠人端「我的商品 - 浏览量」弹窗）
 *
 * <p><b>为什么不是只返回一个数字：</b>摊位上真正有用的问题不是「一共多少人看过」，
 * 而是「这几天有没有人看」。总数只能说明历史，趋势才能说明现在。</p>
 *
 * <p><b>数据来源与口径：</b></p>
 * <ul>
 *   <li>{@code points} 来自 {@code browse_history}，该表按 (user, target_type, target_id)
 *       唯一，重复浏览只刷新 {@code browse_time}，所以「按天 COUNT」统计的是
 *       <b>当天有多少人看过</b>（去重人数），不是被看次数；没有记录的天补 0；</li>
 *   <li>{@code total} 是商品表 {@code view_count} 加上 Redis 里未回写的增量，
 *       与前台详情页显示的累计访问量一致。</li>
 * </ul>
 *
 * <p>两者刻意不相等：累计访问量包含未登录与爬虫等一切请求，
 * 而趋势只统计有行为记录的登录用户。把趋势的求和硬凑成总数，反而是造假数据。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "商品浏览量趋势")
public class GoodsViewTrendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "累计浏览量（商品表值 + Redis 未回写增量）")
    private Integer total;

    @Schema(description = "按天补零后的浏览点，日期升序")
    private List<Point> points;

    @Data
    @Schema(description = "单日浏览点")
    public static class Point implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "日期 yyyy-MM-dd")
        private String date;

        @Schema(description = "日期文案，如 10月27日")
        private String dateText;

        @Schema(description = "当日浏览人数（同一用户只计一次）")
        private Integer count;
    }
}
