package com.lmf.heritage.module.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.content.entity.Article;
import com.lmf.heritage.module.content.mapper.ArticleMapper;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.user.dto.HistoryUploadDTO;
import com.lmf.heritage.module.user.entity.BrowseHistory;
import com.lmf.heritage.module.user.mapper.BrowseHistoryMapper;
import com.lmf.heritage.module.user.service.HistoryService;
import com.lmf.heritage.module.user.vo.HistoryGroupVO;
import com.lmf.heritage.module.user.vo.HistoryItemVO;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 浏览历史服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> implements HistoryService {

    /** 单次上报上限，与前端 localStorage 的 50 条对应 */
    private static final int MAX_UPLOAD = 50;
    /** 列表最多返回多少条历史 */
    private static final int MAX_LIST = 100;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MD_FMT = DateTimeFormatter.ofPattern("M月d日");

    private final GoodsMapper goodsMapper;
    private final CourseMapper courseMapper;
    private final ArticleMapper articleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int upload(HistoryUploadDTO dto) {
        Long userId = UserContext.userId();
        List<HistoryUploadDTO.Item> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            return 0;
        }
        if (items.size() > MAX_UPLOAD) {
            log.warn("浏览历史上报超过 {} 条，已截断。userId={}, size={}", MAX_UPLOAD, userId, items.size());
            items = items.subList(0, MAX_UPLOAD);
        }

        // 一次查出该用户已有的历史，在内存里做去重判断，避免每条都回查一次
        Map<String, BrowseHistory> existMap = list(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(h -> key(h.getTargetType(), h.getTargetId()),
                        h -> h, (a, b) -> a));

        List<BrowseHistory> toInsert = new ArrayList<>();
        List<BrowseHistory> toUpdate = new ArrayList<>();
        for (HistoryUploadDTO.Item item : items) {
            LocalDateTime time = item.getBrowseTime() == null ? LocalDateTime.now() : item.getBrowseTime();
            BrowseHistory exist = existMap.get(key(item.getTargetType(), item.getTargetId()));
            if (exist == null) {
                BrowseHistory h = new BrowseHistory();
                h.setUserId(userId);
                h.setTargetType(item.getTargetType());
                h.setTargetId(item.getTargetId());
                h.setBrowseTime(time);
                toInsert.add(h);
            } else {
                // 已存在：只刷新浏览时间（去重生效，不产生重复行）
                exist.setBrowseTime(time);
                toUpdate.add(exist);
            }
        }
        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            updateBatchById(toUpdate);
        }
        log.info("浏览历史上报完成: userId={}, 新增={}, 去重更新={}", userId, toInsert.size(), toUpdate.size());
        return toInsert.size() + toUpdate.size();
    }

    @Override
    public List<HistoryGroupVO> listGrouped(Integer targetType) {
        Long userId = UserContext.userId();
        List<BrowseHistory> histories = list(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId)
                .eq(targetType != null, BrowseHistory::getTargetType, targetType)
                .orderByDesc(BrowseHistory::getBrowseTime)
                .orderByDesc(BrowseHistory::getId)
                .last("LIMIT " + MAX_LIST));
        if (histories.isEmpty()) {
            return List.of();
        }

        // 按类型收集 id，再对每张表各查一次，补全标题 / 封面 / 价格
        Map<Integer, List<Long>> idsByType = histories.stream()
                .collect(Collectors.groupingBy(BrowseHistory::getTargetType,
                        Collectors.mapping(BrowseHistory::getTargetId, Collectors.toList())));

        Map<Integer, Map<Long, HistoryItemVO>> infoMap = new HashMap<>();
        infoMap.put(Constants.TARGET_GOODS, loadGoodsInfo(idsByType.get(Constants.TARGET_GOODS)));
        infoMap.put(Constants.TARGET_COURSE, loadCourseInfo(idsByType.get(Constants.TARGET_COURSE)));
        infoMap.put(Constants.TARGET_ARTICLE, loadArticleInfo(idsByType.get(Constants.TARGET_ARTICLE)));

        // 组装并按日期分组（LinkedHashMap 保持「最近的日期在前」）
        LinkedHashMap<String, List<HistoryItemVO>> grouped = new LinkedHashMap<>();
        for (BrowseHistory h : histories) {
            HistoryItemVO item = infoMap.getOrDefault(h.getTargetType(), Map.of()).get(h.getTargetId());
            if (item == null) {
                // 目标已被删除（下架/逻辑删除），跳过而不是返回一条空记录
                continue;
            }
            item.setTargetType(h.getTargetType());
            item.setTargetId(h.getTargetId());
            item.setBrowseTime(h.getBrowseTime());
            String date = h.getBrowseTime().toLocalDate().format(DATE_FMT);
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(item);
        }

        LocalDate today = LocalDate.now();
        return grouped.entrySet().stream().map(e -> {
            HistoryGroupVO g = new HistoryGroupVO();
            g.setDate(e.getKey());
            g.setLabel(labelOf(LocalDate.parse(e.getKey(), DATE_FMT), today));
            g.setItems(e.getValue());
            return g;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer targetType, Long targetId) {
        Long userId = UserContext.userId();
        boolean removed = remove(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId)
                .eq(BrowseHistory::getTargetType, targetType)
                .eq(BrowseHistory::getTargetId, targetId));
        BusinessException.throwIf(!removed, ErrorCode.NOT_FOUND);
        log.info("删除浏览历史: userId={}, type={}, targetId={}", userId, targetType, targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear() {
        Long userId = UserContext.userId();
        // browse_history 没有逻辑删除字段，这里是物理删除，条件必须带 userId
        long removed = count(Wrappers.<BrowseHistory>lambdaQuery().eq(BrowseHistory::getUserId, userId));
        remove(Wrappers.<BrowseHistory>lambdaQuery().eq(BrowseHistory::getUserId, userId));
        log.info("清空浏览历史: userId={}, 共删除 {} 条", userId, removed);
    }

    /* ------------------------------------------------------------------ */

    private String key(Integer targetType, Long targetId) {
        return targetType + ":" + targetId;
    }

    private String labelOf(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return "今天";
        }
        if (date.equals(today.minusDays(1))) {
            return "昨天";
        }
        return date.format(MD_FMT);
    }

    private Map<Long, HistoryItemVO> loadGoodsInfo(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return goodsMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Goods::getId, g -> {
            HistoryItemVO vo = new HistoryItemVO();
            vo.setTitle(g.getName());
            vo.setCover(g.getCover());
            vo.setPrice(g.getPrice());
            vo.setSubtitle(g.getSubtitle());
            return vo;
        }, (a, b) -> a));
    }

    private Map<Long, HistoryItemVO> loadCourseInfo(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Course::getId, c -> {
            HistoryItemVO vo = new HistoryItemVO();
            vo.setTitle(c.getTitle());
            vo.setCover(c.getCover());
            vo.setPrice(c.getPrice());
            vo.setSubtitle(c.getSubtitle());
            return vo;
        }, (a, b) -> a));
    }

    private Map<Long, HistoryItemVO> loadArticleInfo(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return articleMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Article::getId, a -> {
            HistoryItemVO vo = new HistoryItemVO();
            vo.setTitle(a.getTitle());
            vo.setCover(a.getCover());
            vo.setSubtitle(a.getSummary());
            return vo;
        }, (a, b) -> a));
    }
}
