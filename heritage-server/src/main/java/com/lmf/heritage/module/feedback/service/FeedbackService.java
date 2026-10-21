package com.lmf.heritage.module.feedback.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.feedback.dto.FeedbackDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackHandleDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackQueryDTO;
import com.lmf.heritage.module.feedback.entity.Feedback;
import com.lmf.heritage.module.feedback.vo.FeedbackVO;

/**
 * 留言反馈服务
 *
 * @author 李明峰 23100025
 */
public interface FeedbackService extends IService<Feedback> {

    /** 提交反馈（登录用户自动带上 userId，未登录也可提交） */
    void submit(FeedbackDTO dto);

    /** 我的反馈（只看自己提交的） */
    PageResult<FeedbackVO> mine(FeedbackQueryDTO query);

    /** 管理端：全部反馈（可按状态与类型筛选） */
    PageResult<FeedbackVO> adminPage(FeedbackQueryDTO query);

    /**
     * 管理端：处理反馈（状态流转 + 回复）
     *
     * <p>流转到「已完成」时必须有回复，否则用户看到「已完成」却没有任何说明。
     * 同时给提交人发一条站内消息（匿名反馈没有 userId，则跳过）。</p>
     */
    void handle(FeedbackHandleDTO dto);
}
