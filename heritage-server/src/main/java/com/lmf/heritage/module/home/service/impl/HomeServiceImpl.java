package com.lmf.heritage.module.home.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.content.service.BannerService;
import com.lmf.heritage.module.content.vo.BannerVO;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.course.vo.CourseSimpleVO;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.mapper.CraftsmanMapper;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.goods.service.CategoryService;
import com.lmf.heritage.module.goods.service.GoodsService;
import com.lmf.heritage.module.goods.vo.CategoryVO;
import com.lmf.heritage.module.goods.vo.GoodsListVO;
import com.lmf.heritage.module.home.service.HomeService;
import com.lmf.heritage.util.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 首页聚合服务实现
 *
 * <p>每个板块一个缓存 key，各自独立失效 —— 改轮播不会把商品缓存也冲掉。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    /** 首页各板块的展示条数 */
    private static final int BANNER_LIMIT = 5;
    private static final int CRAFTSMAN_LIMIT = 4;
    private static final int COURSE_LIMIT = 4;

    /** 轮播位置：1首页 */
    private static final int POSITION_HOME = 1;
    /** 课程状态：2已结束（首页预告要排除） */
    private static final int COURSE_ENDED = 2;
    /** 审核通过 */
    private static final int AUDIT_PASS = 1;

    private final BannerService bannerService;
    private final CraftsmanMapper craftsmanMapper;
    private final CourseMapper courseMapper;
    private final GoodsMapper goodsMapper;
    private final CategoryService categoryService;
    private final GoodsService goodsService;
    private final CacheHelper cache;

    @Override
    public List<BannerVO> banners() {
        // 轮播的「启用 + 排序 + 缓存」逻辑统一由 BannerService 负责，
        // 这里只传位置与条数，避免同一份规则在首页聚合与独立接口里各写一遍
        return bannerService.listByPosition(POSITION_HOME, BANNER_LIMIT);
    }

    @Override
    public List<CategoryVO> categories() {
        // 与分类接口共用 home:categories 缓存，同一份数据不存两份
        return categoryService.listEnabled();
    }

    @Override
    public List<GoodsListVO> hotGoods() {
        return goodsService.hotGoods(8);
    }

    @Override
    public List<CraftsmanSimpleVO> recommendCraftsmen() {
        return cache.getListOrLoad(Constants.KEY_HOME_CRAFTSMEN, CraftsmanSimpleVO.class,
                Constants.TTL_CRAFTSMEN, () -> {
                    List<Craftsman> list = craftsmanMapper.selectList(Wrappers.<Craftsman>lambdaQuery()
                            .eq(Craftsman::getAuditStatus, AUDIT_PASS)
                            .orderByDesc(Craftsman::getFollowerCount)
                            .orderByAsc(Craftsman::getId)
                            .last("LIMIT " + CRAFTSMAN_LIMIT));
                    if (list.isEmpty()) {
                        return List.of();
                    }
                    // 一次查出各自的在售作品数
                    Map<Long, Integer> goodsCounts = new HashMap<>();
                    for (Craftsman c : list) {
                        goodsCounts.put(c.getId(), (int) goodsMapper.countByCraftsman(c.getId()));
                    }
                    log.debug("[首页] 回源推荐匠人 {} 位", list.size());
                    return list.stream().map(c -> {
                        CraftsmanSimpleVO vo = CraftsmanSimpleVO.from(c);
                        vo.setGoodsCount(goodsCounts.getOrDefault(c.getId(), 0));
                        return vo;
                    }).toList();
                });
    }

    @Override
    public List<CourseSimpleVO> upcomingCourses() {
        return cache.getListOrLoad(Constants.KEY_COURSE_LIST, CourseSimpleVO.class,
                Constants.TTL_COURSE_LIST, () -> {
                    LocalDateTime now = LocalDateTime.now();
                    Page<Course> page = new Page<>(1, COURSE_LIMIT, false);
                    IPage<Course> result = courseMapper.selectPage(page, Wrappers.<Course>lambdaQuery()
                            .eq(Course::getAuditStatus, AUDIT_PASS)
                            .ne(Course::getStatus, COURSE_ENDED)
                            .gt(Course::getStartTime, now)
                            .orderByAsc(Course::getStartTime));
                    List<Course> list = result.getRecords();
                    if (list.isEmpty()) {
                        return List.of();
                    }

                    // 批量取匠人姓名，避免逐条回查
                    List<Long> craftsmanIds = list.stream()
                            .map(Course::getCraftsmanId)
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .toList();
                    Map<Long, Craftsman> craftsmanMap = craftsmanIds.isEmpty()
                            ? Map.of()
                            : craftsmanMapper.selectBatchIds(craftsmanIds).stream()
                            .collect(Collectors.toMap(Craftsman::getId, Function.identity(), (a, b) -> a));

                    log.debug("[首页] 回源体验课程 {} 门", list.size());
                    return list.stream().map(c -> {
                        CourseSimpleVO vo = new CourseSimpleVO();
                        vo.setId(c.getId());
                        vo.setTitle(c.getTitle());
                        vo.setSubtitle(c.getSubtitle());
                        vo.setCover(c.getCover());
                        vo.setPrice(c.getPrice());
                        vo.setAddress(c.getAddress());
                        vo.setStartTime(c.getStartTime());
                        vo.setEndTime(c.getEndTime());
                        vo.setBookingDeadline(c.getBookingDeadline());
                        vo.setTotalQuota(c.getTotalQuota());
                        vo.setRemainQuota(c.getRemainQuota());
                        vo.setDifficulty(c.getDifficulty());
                        vo.setStatus(c.getStatus());
                        vo.setCraftsmanId(c.getCraftsmanId());
                        Craftsman cs = craftsmanMap.get(c.getCraftsmanId());
                        if (cs != null) {
                            vo.setCraftsmanName(cs.getRealName());
                            vo.setCraftType(cs.getCraftType());
                        }
                        return vo;
                    }).toList();
                });
    }

    @Override
    public void evictCache() {
        bannerService.evictCache();
        cache.delete(Constants.KEY_HOME_HOT_GOODS,
                Constants.KEY_HOME_CRAFTSMEN,
                Constants.KEY_COURSE_LIST);
    }
}
