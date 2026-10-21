package com.lmf.heritage.module.feedback.service;

import com.lmf.heritage.module.feedback.dto.FeedbackBatchDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackQueryDTO;
import com.lmf.heritage.module.feedback.vo.FeedbackExcelVO;
import com.lmf.heritage.module.feedback.vo.FeedbackStatVO;

import java.util.List;

/**
 * 管理端 · 留言反馈统计与导出
 *
 * <p>列表与状态流转仍由 {@code FeedbackService} 承担（用户端与管理端共用），
 * 这里只放管理端独有的三件事：处理时效统计、批量处理、导出。</p>
 *
 * @author 李明峰 23100025
 */
public interface AdminFeedbackService {

    /**
     * 处理时效统计
     *
     * @param days 趋势图的天数范围，默认 14，上限 90
     */
    FeedbackStatVO stat(Integer days);

    /** 批量处理反馈，返回生效条数（整批回滚） */
    int batchHandle(FeedbackBatchDTO dto);

    /** 导出：与列表同一套筛选条件，含处理时长列 */
    List<FeedbackExcelVO> exportList(FeedbackQueryDTO query);
}
