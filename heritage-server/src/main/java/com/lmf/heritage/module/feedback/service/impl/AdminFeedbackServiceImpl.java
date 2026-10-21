package com.lmf.heritage.module.feedback.service.impl;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.feedback.dto.FeedbackBatchDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackHandleDTO;
import com.lmf.heritage.module.feedback.dto.FeedbackQueryDTO;
import com.lmf.heritage.module.feedback.mapper.FeedbackMapper;
import com.lmf.heritage.module.feedback.service.AdminFeedbackService;
import com.lmf.heritage.module.feedback.service.FeedbackService;
import com.lmf.heritage.module.feedback.vo.FeedbackExcelVO;
import com.lmf.heritage.module.feedback.vo.FeedbackStatVO;
import com.lmf.heritage.module.feedback.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端 · 留言反馈统计与导出实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 默认趋势天数 */
    private static final int DEFAULT_DAYS = 14;

    /** 趋势天数上限 */
    private static final int MAX_DAYS = 90;

    /** 导出上限 */
    private static final int EXPORT_LIMIT = 5000;

    private static final Map<Integer, String> TYPE_DESC = Map.of(
            1, "功能建议", 2, "内容纠错", 3, "投诉举报", 4, "其他");

    private final FeedbackMapper feedbackMapper;
    private final FeedbackService feedbackService;

    /* ==================== 处理时效统计 ==================== */

    @Override
    public FeedbackStatVO stat(Integer days) {
        int n = (days == null || days <= 0) ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);

        // 只查一遍全量状态分布，不再重复 COUNT 总数：
        // 分布里各项相加就是总数，分开查两次可能查到两个不同时刻的结果
        Map<Integer, Long> statusCount = new HashMap<>();
        for (Map<String, Object> row : feedbackMapper.selectStatusStat()) {
            statusCount.put(toInt(row.get("status")), toLong(row.get("cnt")));
        }
        long total = statusCount.values().stream().mapToLong(Long::longValue).sum();
        long waitCount = statusCount.getOrDefault(Constants.FEEDBACK_WAIT, 0L);
        long processingCount = statusCount.getOrDefault(Constants.FEEDBACK_PROCESSING, 0L);
        long doneCount = statusCount.getOrDefault(Constants.FEEDBACK_DONE, 0L);

        Map<String, Object> duration = feedbackMapper.selectDurationStat();
        long handledCount = toLong(duration.get("handledCount"));
        long within24h = toLong(duration.get("within24h"));

        FeedbackStatVO vo = new FeedbackStatVO();
        vo.setTotal(total);
        vo.setWaitCount(waitCount);
        vo.setProcessingCount(processingCount);
        vo.setDoneCount(doneCount);
        vo.setDoneRate(rate(doneCount, total));
        vo.setHandledCount(handledCount);
        // 没有已完成反馈时保持 null：返回 0 会被读成「平均 0 小时就处理完了」
        vo.setAvgHandleHours(handledCount == 0 ? null : toDecimal(duration.get("avgHours")));
        vo.setMinHandleHours(handledCount == 0 ? null : toDecimal(duration.get("minHours")));
        vo.setMaxHandleHours(handledCount == 0 ? null : toDecimal(duration.get("maxHours")));
        vo.setWithin24hCount(within24h);
        vo.setWithin24hRate(rate(within24h, handledCount));

        List<FeedbackStatVO.TypeStat> typeStats = new ArrayList<>();
        for (Map<String, Object> row : feedbackMapper.selectTypeStat()) {
            Integer type = toInt(row.get("type"));
            FeedbackStatVO.TypeStat ts = new FeedbackStatVO.TypeStat();
            ts.setType(type);
            ts.setTypeDesc(TYPE_DESC.getOrDefault(type, "其他"));
            ts.setCount(toLong(row.get("cnt")));
            ts.setDoneCount(toLong(row.get("doneCount")));
            ts.setAvgHandleHours(toDecimal(row.get("avgHours")));
            typeStats.add(ts);
        }
        vo.setTypeStats(typeStats);

        vo.setDaily(dailyTrend(n));
        return vo;
    }

    /**
     * 近 N 天趋势：把 SQL 只返回「有数据的日期」的结果补成连续 N 天
     *
     * <p>补零不是为了好看。折线图遇到缺失的日期会把两点直接连起来，
     * 于是「1 号 10 条、3 号 10 条」会被画成一条从 1 号斜到 3 号的线，
     * 看起来像 2 号也有数据 —— 图表在说谎比图表难看严重得多。</p>
     */
    private List<FeedbackStatVO.DailyStat> dailyTrend(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        Map<String, long[]> byDay = new HashMap<>();
        for (Map<String, Object> row : feedbackMapper.selectDailyStat(from)) {
            byDay.put(String.valueOf(row.get("day")),
                    new long[]{toLong(row.get("submitCount")), toLong(row.get("doneCount"))});
        }

        List<FeedbackStatVO.DailyStat> list = new ArrayList<>(days);
        LocalDate cursor = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            String key = cursor.toString();
            long[] v = byDay.getOrDefault(key, new long[]{0L, 0L});
            FeedbackStatVO.DailyStat d = new FeedbackStatVO.DailyStat();
            d.setDate(key);
            d.setSubmitCount(v[0]);
            d.setDoneCount(v[1]);
            list.add(d);
            cursor = cursor.plusDays(1);
        }
        return list;
    }

    /* ==================== 批量处理 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchHandle(FeedbackBatchDTO dto) {
        List<Long> ids = dto.getIds().stream().filter(Objects::nonNull).distinct().toList();
        BusinessException.throwIf(ids.isEmpty(), ErrorCode.ADMIN_OPERATION_EMPTY);
        boolean done = Integer.valueOf(Constants.FEEDBACK_DONE).equals(dto.getStatus());
        BusinessException.throwIf(done && (dto.getReply() == null || dto.getReply().isBlank()),
                ErrorCode.PARAM_ERROR, "批量流转为「已完成」时必须填写统一的回复内容");

        // 复用单条处理的完整逻辑（含校验、消息通知），不另写一套 UPDATE：
        // 两套实现迟早会在「要不要发通知」这类细节上分叉
        int affected = 0;
        for (Long id : ids) {
            FeedbackHandleDTO one = new FeedbackHandleDTO();
            one.setId(id);
            one.setStatus(dto.getStatus());
            one.setReply(dto.getReply());
            feedbackService.handle(one);
            affected++;
        }
        log.info("[反馈管理] 批量处理 {} 条，目标状态 {}", affected, dto.getStatus());
        return affected;
    }

    /* ==================== 导出 ==================== */

    @Override
    public List<FeedbackExcelVO> exportList(FeedbackQueryDTO query) {
        query.normalize();
        // 直接复用管理端分页查询：类型/状态描述与处理时长都基于同一份已经装饰好的数据，
        // 导出与页面不可能对不上
        query.setPageNum(1L);
        query.setPageSize((long) EXPORT_LIMIT);
        PageResult<FeedbackVO> page = feedbackService.adminPage(query);
        return page.getRecords().stream().map(v -> {
            FeedbackExcelVO e = new FeedbackExcelVO();
            e.setId(v.getId());
            e.setUsername(v.getUsername());
            e.setNickname(v.getNickname());
            e.setTypeDesc(v.getTypeDesc());
            e.setContent(v.getContent());
            e.setReply(v.getReply());
            e.setStatusDesc(v.getStatusDesc());
            e.setCreateTimeText(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            e.setHandleTimeText(v.getHandleTime() == null ? null : v.getHandleTime().format(DT));
            e.setHandleHours(handleHours(v.getCreateTime(), v.getHandleTime()));
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== 内部工具 ==================== */

    /** 处理时长（小时，保留 1 位）；没有完成时间时为 null */
    private static BigDecimal handleHours(LocalDateTime createTime, LocalDateTime handleTime) {
        if (createTime == null || handleTime == null) {
            return null;
        }
        return BigDecimal.valueOf(Duration.between(createTime, handleTime).toMinutes())
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /**
     * MySQL 的 COUNT / SUM / TIMESTAMPDIFF 在不同版本下可能返回 Long 或 BigDecimal，
     * 统一按 Number 取值 —— 否则 ClassCastException 只会在某台机器上复现，很难定位。
     */
    private static long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
    }

    private static int toInt(Object v) {
        return (int) toLong(v);
    }

    private static BigDecimal toDecimal(Object v) {
        if (v == null) {
            return null;
        }
        return v instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(v));
    }
}
