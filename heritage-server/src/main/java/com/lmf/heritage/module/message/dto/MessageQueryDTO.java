package com.lmf.heritage.module.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站内消息查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "站内消息查询条件")
public class MessageQueryDTO {

    @Schema(description = "消息类型 1预约审核 2订单发货 3退款 4优惠券 5系统公告 6预约提醒，不传表示全部")
    private Integer type;

    @Schema(description = "是否只看未读 0全部 1只看未读")
    private Integer onlyUnread;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    /** 归一化分页参数 */
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
    }
}
