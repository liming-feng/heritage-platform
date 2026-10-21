package com.lmf.heritage.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 单门课程的名额使用率
 *
 * <p><b>这里为什么要同时返回 three 个数字而不是只给一个百分比</b>：
 * 百分比是算出来的，一旦与页面上的「总名额 / 剩余名额」对不上，光看百分比查不出是谁错了。
 * 把 {@code totalQuota}、{@code remainQuota}、{@code usedQuota} 全部返回，
 * 验收时 {@code usedQuota = totalQuota − remainQuota} 可以直接用眼睛核。</p>
 *
 * <p>{@code usedQuota} 由预约表现算而不取 {@code totalQuota − remainQuota}，
 * 两者理论上必须相等——这不是冗余，而是<b>交叉校验</b>：
 * 一旦某个归还名额的分支漏了更新 {@code remain_quota}，这个字段就会和差值对不上，
 * 看板上会直接暴露出来。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "课程名额使用率")
public class CourseQuotaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "课程 id")
    private Long courseId;

    @Schema(description = "课程标题")
    private String courseTitle;

    @Schema(description = "开课时间")
    private LocalDateTime startTime;

    @Schema(description = "课程状态 0未开始 1进行中 2已结束")
    private Integer status;

    @Schema(description = "课程状态文案")
    private String statusDesc;

    @Schema(description = "总名额")
    private Integer totalQuota;

    @Schema(description = "剩余名额")
    private Integer remainQuota;

    @Schema(description = "已占用名额（由预约表现算，应等于 总名额 − 剩余名额）")
    private Long usedQuota;

    @Schema(description = "名额使用率（0-100，1 位小数）")
    private Double usedRate;

    @Schema(description = "预约单数")
    private Long bookingCount;
}
