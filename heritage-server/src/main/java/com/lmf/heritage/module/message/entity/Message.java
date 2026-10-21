package com.lmf.heritage.module.message.entity;

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
 * 站内消息
 *
 * <p>预约的每一次状态变更都要落一条消息：审核通过 / 审核拒绝 / 用户取消 / 管理员取消 /
 * 系统超时自动拒绝 / 开课提醒。这张表是「状态机和用户之间」的桥梁——
 * 用户不需要反复刷新去猜预约进展。</p>
 *
 * <p>{@code relatedId} 存关联的业务 id（预约消息存 booking.id），
 * 前端点消息可以直接跳到对应详情页。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("message")
@Schema(description = "站内消息")
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "接收人 user.id")
    private Long userId;

    @Schema(description = "消息类型 1预约审核 2订单发货 3退款 4优惠券 5系统公告 6预约提醒")
    private Integer type;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "关联业务 id")
    private Long relatedId;

    @Schema(description = "是否已读 0未读 1已读")
    private Integer isRead;

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
