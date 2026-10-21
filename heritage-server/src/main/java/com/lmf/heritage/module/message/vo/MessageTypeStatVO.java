package com.lmf.heritage.module.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息分类统计（消息中心分类 tab 的角标）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "消息分类统计")
public class MessageTypeStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息类型 1预约审核 2订单发货 3退款 4优惠券 5系统公告 6预约提醒")
    private Integer type;

    @Schema(description = "类型描述")
    private String typeDesc;

    @Schema(description = "该分类消息总数")
    private Long total;

    @Schema(description = "该分类未读数")
    private Long unread;
}
