package com.lmf.heritage.module.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息视图
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "站内消息")
public class MessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息 id")
    private Long id;

    @Schema(description = "消息类型 1预约审核 2订单发货 3退款 4优惠券 5系统公告 6预约提醒")
    private Integer type;

    @Schema(description = "类型名称")
    private String typeDesc;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "关联业务 id")
    private Long relatedId;

    @Schema(description = "是否已读 0未读 1已读")
    private Integer isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
