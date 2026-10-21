package com.lmf.heritage.module.booking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 核销签到结果
 *
 * <p>匠人端核销成功后需要一眼看清「是谁、哪门课、几个人」，
 * 所以这里把关键信息回带，而不是只返回一个成功标志。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "核销签到结果")
public class CheckinResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "预约 id")
    private Long bookingId;

    @Schema(description = "预约单号")
    private String bookingNo;

    @Schema(description = "课程标题")
    private String courseTitle;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "预约人账号昵称（签到台上核对身份用）")
    private String nickname;

    @Schema(description = "预约提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "上课地点")
    private String courseAddress;

    @Schema(description = "开课时间")
    private LocalDateTime courseStartTime;

    @Schema(description = "核销名额数")
    private Integer quota;

    @Schema(description = "本次核销方式：1 输入/扫码核销码，2 名单手动核销")
    private Integer checkinWay;

    @Schema(description = "核销时间")
    private LocalDateTime checkinTime;

    @Schema(description = "核销后状态名称")
    private String statusDesc;
}
