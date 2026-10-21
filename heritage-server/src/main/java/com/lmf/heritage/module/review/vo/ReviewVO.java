package com.lmf.heritage.module.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价展示对象
 *
 * <p>{@code images} 在库里是 JSON 字符串，这里转成数组返回给前端
 * （转换在 ReviewSupport 里做，避免 VO 里写解析逻辑）。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "评价")
public class ReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评价 id")
    private Long id;

    @Schema(description = "类型 1商品评价 2课程评价")
    private Integer type;

    @Schema(description = "评价对象 id")
    private Long targetId;

    @Schema(description = "评分 1-5")
    private Integer rating;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "配图 URL 列表（最多 3 张）")
    private List<String> images;

    @Schema(description = "匠人回复")
    private String reply;

    @Schema(description = "回复时间")
    private LocalDateTime replyTime;

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
