package com.lmf.heritage.module.message.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.message.dto.MessageQueryDTO;
import com.lmf.heritage.module.message.service.MessageService;
import com.lmf.heritage.module.message.vo.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 · 站内消息
 *
 * <p>预约的每一次状态变更都会在这里留下一条记录：提交、审核通过、审核拒绝、
 * 用户取消、匠人取消、管理员取消、系统超时自动拒绝、开课提醒。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-消息", description = "消息列表 / 未读数 / 标记已读")
@RestController
@RequestMapping("/portal/message")
@RequiredArgsConstructor
@LoginRequired
public class PortalMessageController {

    private final MessageService messageService;

    @Operation(summary = "消息列表", description = "支持类型 type 筛选与只看未读 onlyUnread=1")
    @GetMapping("/list")
    public Result<PageResult<MessageVO>> list(MessageQueryDTO query) {
        return Result.success(messageService.page(query));
    }

    @Operation(summary = "未读消息数", description = "个人中心红点用")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(messageService.unreadCount());
    }

    @Operation(summary = "分类统计",
            description = "返回 6 个消息分类各自的总数与未读数（没有消息的分类也会返回 total=0），"
                    + "用于消息中心的分类 tab 与小红点")
    @GetMapping("/type-stat")
    public Result<java.util.List<com.lmf.heritage.module.message.vo.MessageTypeStatVO>> typeStat() {
        return Result.success(messageService.typeStat());
    }

    @Operation(summary = "标记单条已读", description = "只能标记自己的消息")
    @PostMapping("/read/{id}")
    public Result<Void> read(@PathVariable("id") Long id) {
        messageService.markRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读", description = "返回本次标记的条数")
    @PostMapping("/read-all")
    public Result<Integer> readAll() {
        return Result.success(messageService.markAllRead());
    }
}
