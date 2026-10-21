package com.lmf.heritage.module.course.entity;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 非遗体验课程（预约业务的载体）
 *
 * <p><b>题眼相关</b>：{@code totalQuota} / {@code remainQuota} 是名额控制的落点，
 * P5 会在此基础上做「Redis 预扣 + 数据库乐观锁 + 唯一索引」三层防超卖。
 * {@code bookingDeadline} / {@code cancelDeadline} 决定预约与取消是否还在时限内。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("course")
@Schema(description = "非遗体验课程")
public class Course implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "授课匠人 craftsman.id")
    private Long craftsmanId;

    @Schema(description = "关联非遗分类 category.id")
    private Long categoryId;

    @Schema(description = "课程标题")
    private String title;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "课程简介")
    private String description;

    @Schema(description = "课程详情（富文本）")
    private String detail;

    @Schema(description = "上课地点")
    private String address;

    @Schema(description = "开课时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "课程费用")
    private BigDecimal price;

    @Schema(description = "总名额")
    private Integer totalQuota;

    @Schema(description = "剩余名额")
    private Integer remainQuota;

    @Schema(description = "预约截止时间")
    private LocalDateTime bookingDeadline;

    @Schema(description = "可取消截止时间")
    private LocalDateTime cancelDeadline;

    @Schema(description = "课时（分钟）")
    private Integer duration;

    @Schema(description = "难度 1入门 2进阶 3高级")
    private Integer difficulty;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "累计预约数")
    private Integer bookingCount;

    @Schema(description = "状态 0未开始 1进行中 2已结束")
    private Integer status;

    @Schema(description = "审核状态 0待审核 1通过 2驳回")
    private Integer auditStatus;

    @Schema(description = "驳回原因")
    private String rejectReason;

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
