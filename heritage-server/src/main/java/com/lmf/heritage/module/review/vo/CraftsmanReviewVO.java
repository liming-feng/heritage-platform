package com.lmf.heritage.module.review.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 匠人端评价项
 *
 * <p>比用户端 {@code ReviewVO} 多出 {@code targetName} 与 {@code targetCover}：
 * 匠人看的是「哪件商品 / 哪门课被评了」，如果只有 {@code targetId}，
 * 前端还得再为每条评价补一次查询。这里在联表时一次带出。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人端评价项")
public class CraftsmanReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评价 id")
    private Long id;

    @Schema(description = "类型 1商品评价 2课程评价")
    private Integer type;

    @Schema(description = "类型名称")
    private String typeDesc;

    @Schema(description = "评价对象 id")
    private Long targetId;

    @Schema(description = "评价对象名称（商品名或课程名）")
    private String targetName;

    @Schema(description = "评价对象封面")
    private String targetCover;

    @Schema(description = "评分 1-5")
    private Integer rating;

    @Schema(description = "口碑档位：good 好评 / mid 中评 / bad 差评")
    private String ratingLevel;

    @Schema(description = "评价内容")
    private String content;

    /**
     * 数据库里的原始配图字段（JSON 数组字符串）
     *
     * <p>MyBatis 只能把这一列映射成字符串，所以先落到这里，
     * 再由服务层解析成 {@link #images}。不参与序列化——对外只暴露解析好的数组。</p>
     */
    @JsonIgnore
    @Schema(hidden = true)
    private String imagesJson;

    @Schema(description = "配图 URL 列表（最多 3 张）")
    private List<String> images;

    @Schema(description = "是否带图")
    private Boolean hasImage;

    @Schema(description = "匠人回复")
    private String reply;

    @Schema(description = "回复时间")
    private LocalDateTime replyTime;

    @Schema(description = "是否已回复")
    private Boolean replied;

    @Schema(description = "有用数")
    private Integer likeCount;

    @Schema(description = "评价人 id")
    private Long userId;

    @Schema(description = "评价人昵称")
    private String nickname;

    @Schema(description = "评价人头像")
    private String avatar;

    @Schema(description = "评价时间")
    private LocalDateTime createTime;
}
