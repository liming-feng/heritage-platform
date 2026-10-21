package com.lmf.heritage.module.review.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一评价表（商品评价 / 课程评价共用，用 type 区分）
 *
 * <p>规划书 7.5：不拆两张表，靠 {@code type} + {@code targetId} 定位评价对象，
 * 商品评价关联 orderId、课程评价关联 bookingId。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("review")
@Schema(description = "评价（商品 / 课程共用）")
public class Review implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "类型 1商品评价 2课程评价")
    private Integer type;

    @Schema(description = "评价对象：商品 id 或课程 id")
    private Long targetId;

    @Schema(description = "关联订单（商品评价）")
    private Long orderId;

    @Schema(description = "关联预约（课程评价）")
    private Long bookingId;

    @Schema(description = "评价人 user.id")
    private Long userId;

    @Schema(description = "评分 1-5")
    private Integer rating;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "图片 URL 列表（JSON，最多 3 张）")
    private String images;

    @Schema(description = "匠人回复")
    private String reply;

    @Schema(description = "回复时间")
    private LocalDateTime replyTime;

    @Schema(description = "有用数")
    private Integer likeCount;

    @JsonIgnore
    @TableLogic
    @Schema(description = "逻辑删除 0未删 1已删")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
