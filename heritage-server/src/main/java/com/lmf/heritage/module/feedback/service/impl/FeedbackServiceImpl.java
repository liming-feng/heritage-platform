package com.lmf.heritage.module.feedback.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.feedback.dto.FeedbackDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackHandleDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackQueryDTO;
import com.lmf.heritage.module.feedback.entity.Feedback;
import com.lmf.heritage.module.feedback.mapper.FeedbackMapper;
import com.lmf.heritage.module.feedback.service.FeedbackService;
import com.lmf.heritage.module.feedback.vo.FeedbackVO;
import com.lmf.heritage.module.message.service.MessageService;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 留言反馈服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    private static final Map<Integer, String> TYPE_DESC = Map.of(
            1, "功能建议", 2, "内容纠错", 3, "投诉举报", 4, "其他");

    private static final Map<Integer, String> STATUS_DESC = Map.of(
            Constants.FEEDBACK_WAIT, "待处理",
            Constants.FEEDBACK_PROCESSING, "处理中",
            Constants.FEEDBACK_DONE, "已完成");

    private final FeedbackMapper feedbackMapper;
    private final MessageService messageService;

    @Override
    public void submit(FeedbackDTO dto) {
        // 未登录也能提交：这里不能用 UserContext.userId()（那会抛未登录），而是取「有就带上」
        Long userId = UserContext.userIdOrNull();

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(dto.getType());
        feedback.setContent(dto.getContent());
        feedback.setContact(dto.getContact());
        feedback.setStatus(Constants.FEEDBACK_WAIT);
        feedbackMapper.insert(feedback);

        log.info("[反馈] 用户 {} 提交反馈 #{}（类型 {}）", userId, feedback.getId(), dto.getType());
    }

    @Override
    public PageResult<FeedbackVO> mine(FeedbackQueryDTO query) {
        query.normalize();
        Page<FeedbackVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<FeedbackVO> result = feedbackMapper.selectFeedbackPage(page, UserContext.userId(), query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result, result.getRecords());
    }

    @Override
    public PageResult<FeedbackVO> adminPage(FeedbackQueryDTO query) {
        query.normalize();
        Page<FeedbackVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        // userId 传 null 表示不过滤提交人 → 管理端看全量
        IPage<FeedbackVO> result = feedbackMapper.selectFeedbackPage(page, null, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result, result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(FeedbackHandleDTO dto) {
        Feedback feedback = feedbackMapper.selectById(dto.getId());
        BusinessException.throwIf(feedback == null, ErrorCode.FEEDBACK_NOT_FOUND);

        // 标「已完成」却不写回复，用户点进来只会看到一句「已完成」而不知道处理结果
        boolean done = Integer.valueOf(Constants.FEEDBACK_DONE).equals(dto.getStatus());
        BusinessException.throwIf(done && !StringUtils.hasText(dto.getReply()),
                ErrorCode.PARAM_ERROR, "流转为「已完成」时必须填写回复内容");

        int rows = feedbackMapper.handle(dto.getId(), dto.getStatus(), dto.getReply(), LocalDateTime.now());
        BusinessException.throwIf(rows == 0, ErrorCode.FEEDBACK_NOT_FOUND);

        // 只有「已完成」才通知提交人：处理中的中间状态对用户没有信息量
        if (done && feedback.getUserId() != null) {
            messageService.send(feedback.getUserId(), Constants.MSG_SYSTEM, "留言反馈已处理",
                    "你提交的反馈已处理完成，回复：" + dto.getReply(),
                    feedback.getId());
        }

        log.info("[反馈] 反馈 #{} 状态 {} → {}，回复={}",
                dto.getId(), feedback.getStatus(), dto.getStatus(), dto.getReply());
    }

    private void decorate(FeedbackVO vo) {
        vo.setTypeDesc(TYPE_DESC.getOrDefault(vo.getType(), "其他"));
        vo.setStatusDesc(STATUS_DESC.getOrDefault(vo.getStatus(), "未知"));
    }
}
