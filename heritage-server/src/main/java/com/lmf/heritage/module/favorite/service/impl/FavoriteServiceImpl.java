package com.lmf.heritage.module.favorite.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.entity.Article;
import com.lmf.heritage.module.content.mapper.ArticleMapper;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.favorite.dto.FavoriteDTO;
import com.lmf.heritage.module.favorite.dto.FavoriteQueryDTO;
import com.lmf.heritage.module.favorite.entity.Favorite;
import com.lmf.heritage.module.favorite.mapper.FavoriteMapper;
import com.lmf.heritage.module.favorite.service.FavoriteService;
import com.lmf.heritage.module.favorite.vo.FavoriteVO;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 收藏服务实现
 *
 * <p><b>为什么收藏前要回查目标：</b>收藏本质上是在用户和目标之间建立一条引用，
 * 如果目标不存在（或已下架），这条引用就是脏数据，「我的收藏」里会出现一条
 * 点进去 404 的卡片。所以在入口处就把目标查一次——代价是一次主键查询。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private static final Map<Integer, String> TYPE_DESC = Map.of(
            Constants.TARGET_GOODS, "商品",
            Constants.TARGET_COURSE, "课程",
            Constants.TARGET_ARTICLE, "资讯");

    /** 商品上架状态 */
    private static final int GOODS_ON_SHELF = 1;
    /** 审核通过 */
    private static final int AUDIT_PASS = 1;

    private final FavoriteMapper favoriteMapper;
    private final GoodsMapper goodsMapper;
    private final CourseMapper courseMapper;
    private final ArticleMapper articleMapper;

    @Override
    public void add(FavoriteDTO dto) {
        Long userId = UserContext.userId();
        int targetType = dto.getTargetType();

        // 校验目标存在且对用户可见，避免收藏成功后卡片点不进去
        requireTargetVisible(targetType, dto.getTargetId());

        // 幂等 upsert：首次收藏是 insert，取消后再收藏是把老记录复活（唯一键不允许插第二条）
        favoriteMapper.upsert(userId, targetType, dto.getTargetId());
        log.debug("[收藏] 用户 {} 收藏{} {}", userId, TYPE_DESC.getOrDefault(targetType, ""), dto.getTargetId());
    }

    @Override
    public void remove(FavoriteDTO dto) {
        Long userId = UserContext.userId();
        int rows = favoriteMapper.remove(userId, dto.getTargetType(), dto.getTargetId());
        if (rows == 0) {
            log.debug("[收藏] 用户 {} 未收藏目标 {}/{}，取消操作忽略",
                    userId, dto.getTargetType(), dto.getTargetId());
        }
    }

    @Override
    public boolean isFavorited(Integer targetType, Long targetId, Long userId) {
        if (userId == null || targetType == null || targetId == null) {
            return false;
        }
        return favoriteMapper.countByUserAndTarget(userId, targetType, targetId) > 0;
    }

    @Override
    public PageResult<FavoriteVO> mine(FavoriteQueryDTO query) {
        query.normalize();
        Page<FavoriteVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<FavoriteVO> result = favoriteMapper.selectMyPage(page, UserContext.userId(), query.getTargetType());
        result.getRecords().forEach(vo -> vo.setTargetTypeDesc(TYPE_DESC.getOrDefault(vo.getTargetType(), "其他")));
        return PageResult.of(result, result.getRecords());
    }

    @Override
    public Map<Integer, Integer> myCount() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(Constants.TARGET_GOODS, 0);
        map.put(Constants.TARGET_COURSE, 0);
        map.put(Constants.TARGET_ARTICLE, 0);
        for (Map<String, Object> row : favoriteMapper.countGroupByType(UserContext.userId())) {
            Object type = row.get("targetType");
            Object cnt = row.get("cnt");
            if (type instanceof Number t && cnt instanceof Number c) {
                map.put(t.intValue(), c.intValue());
            }
        }
        return map;
    }

    /* ==================== 内部方法 ==================== */

    /** 校验收藏目标存在且可见，不可见时按「不存在」处理（不泄露「有这个东西但下架了」） */
    private void requireTargetVisible(int targetType, Long targetId) {
        if (Integer.valueOf(Constants.TARGET_GOODS).equals(targetType)) {
            Goods goods = goodsMapper.selectById(targetId);
            BusinessException.throwIf(goods == null
                            || !Integer.valueOf(GOODS_ON_SHELF).equals(goods.getStatus()),
                    ErrorCode.GOODS_NOT_FOUND);
        } else if (Integer.valueOf(Constants.TARGET_COURSE).equals(targetType)) {
            Course course = courseMapper.selectById(targetId);
            BusinessException.throwIf(course == null
                            || !Integer.valueOf(AUDIT_PASS).equals(course.getAuditStatus()),
                    ErrorCode.COURSE_NOT_FOUND);
        } else if (Integer.valueOf(Constants.TARGET_ARTICLE).equals(targetType)) {
            Article article = articleMapper.selectById(targetId);
            BusinessException.throwIf(article == null
                            || !Integer.valueOf(Constants.ARTICLE_PUBLISHED).equals(article.getStatus()),
                    ErrorCode.ARTICLE_NOT_FOUND);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的收藏类型：" + targetType);
        }
    }
}
