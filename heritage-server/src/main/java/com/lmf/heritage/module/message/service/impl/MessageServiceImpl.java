package com.lmf.heritage.module.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.message.dto.MessageQueryDTO;
import com.lmf.heritage.module.message.entity.Message;
import com.lmf.heritage.module.message.mapper.MessageMapper;
import com.lmf.heritage.module.message.service.MessageService;
import com.lmf.heritage.module.message.vo.MessageTypeStatVO;
import com.lmf.heritage.module.message.vo.MessageVO;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;

    private static final java.util.Map<Integer, String> TYPE_DESC = java.util.Map.of(
            1, "预约审核", 2, "订单发货", 3, "退款", 4, "优惠券", 5, "系统公告", 6, "预约提醒");

    @Override
    public void send(Long userId, int type, String title, String content, Long relatedId) {
        if (userId == null) {
            return;
        }
        try {
            Message m = new Message();
            m.setUserId(userId);
            m.setType(type);
            m.setTitle(title);
            m.setContent(content);
            m.setRelatedId(relatedId);
            m.setIsRead(0);
            messageMapper.insert(m);
            log.debug("[消息] 已发送给用户 {} type={} title={}", userId, type, title);
        } catch (Exception e) {
            // 消息只是「通知」，不能因为通知失败就把预约 / 审核这些主业务回滚掉
            log.warn("[消息] 发送失败（已忽略，不影响主业务）userId={} type={} err={}", userId, type, e.getMessage());
        }
    }

    @Override
    public PageResult<MessageVO> page(MessageQueryDTO query) {
        query.normalize();
        Long userId = UserContext.userId();

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(query.getType() != null, Message::getType, query.getType())
                .eq(Integer.valueOf(1).equals(query.getOnlyUnread()), Message::getIsRead, 0)
                .orderByDesc(Message::getId);

        Page<Message> page = messageMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page, toVOList(page.getRecords()));
    }

    @Override
    public long unreadCount() {
        return messageMapper.countUnread(UserContext.userId());
    }

    @Override
    public List<MessageTypeStatVO> typeStat() {
        Long userId = UserContext.userId();
        Map<Integer, long[]> statMap = new HashMap<>();
        for (Map<String, Object> row : messageMapper.countGroupByType(userId)) {
            Object type = row.get("type");
            if (!(type instanceof Number t)) {
                continue;
            }
            long total = row.get("total") instanceof Number n ? n.longValue() : 0L;
            long unread = row.get("unread") instanceof Number n ? n.longValue() : 0L;
            statMap.put(t.intValue(), new long[]{total, unread});
        }

        // 没有消息的分类也要返回（total=0），前端才能渲染出完整的 tab 列表，
        // 而不是「用过一次优惠券之后才冒出一个优惠券 tab」
        List<MessageTypeStatVO> list = new ArrayList<>(TYPE_DESC.size());
        for (Map.Entry<Integer, String> entry : TYPE_DESC.entrySet()) {
            long[] counts = statMap.getOrDefault(entry.getKey(), new long[]{0L, 0L});
            MessageTypeStatVO vo = new MessageTypeStatVO();
            vo.setType(entry.getKey());
            vo.setTypeDesc(entry.getValue());
            vo.setTotal(counts[0]);
            vo.setUnread(counts[1]);
            list.add(vo);
        }
        list.sort(java.util.Comparator.comparing(MessageTypeStatVO::getType));
        return list;
    }

    @Override
    public void markRead(Long messageId) {
        Long userId = UserContext.userId();
        Message m = messageMapper.selectById(messageId);
        BusinessException.throwIf(m == null, ErrorCode.NOT_FOUND, "消息不存在");
        // 只能标记自己的消息，防止拿到别人的 id 改别人的状态
        BusinessException.throwIf(!userId.equals(m.getUserId()), ErrorCode.NO_PERMISSION);
        if (Integer.valueOf(1).equals(m.getIsRead())) {
            return;
        }
        Message update = new Message();
        update.setId(messageId);
        update.setIsRead(1);
        messageMapper.updateById(update);
    }

    @Override
    public int markAllRead() {
        return messageMapper.markAllRead(UserContext.userId());
    }

    @Override
    public boolean alreadySent(Long userId, int type, Long relatedId) {
        if (relatedId == null) {
            return false;
        }
        return messageMapper.countByRelated(userId, type, relatedId) > 0;
    }

    @Override
    public boolean alreadySent(Long userId, int type, Long relatedId, String title) {
        if (relatedId == null || title == null || title.isBlank()) {
            // 标题缺失时退回「同类型同单据」的宽松判断，宁可漏发也不重复轰炸用户
            return alreadySent(userId, type, relatedId);
        }
        return messageMapper.countByRelatedAndTitle(userId, type, relatedId, title) > 0;
    }

    /* ==================== 内部方法 ==================== */

    private List<MessageVO> toVOList(List<Message> list) {
        List<MessageVO> result = new ArrayList<>(list.size());
        for (Message m : list) {
            MessageVO vo = new MessageVO();
            vo.setId(m.getId());
            vo.setType(m.getType());
            vo.setTypeDesc(TYPE_DESC.getOrDefault(m.getType(), "通知"));
            vo.setTitle(m.getTitle());
            vo.setContent(m.getContent());
            vo.setRelatedId(m.getRelatedId());
            vo.setIsRead(m.getIsRead());
            vo.setCreateTime(m.getCreateTime());
            result.add(vo);
        }
        return result;
    }
}
