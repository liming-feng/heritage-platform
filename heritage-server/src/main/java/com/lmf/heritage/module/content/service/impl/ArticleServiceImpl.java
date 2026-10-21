package com.lmf.heritage.module.content.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.dto.ArticleQueryDTO;
import com.lmf.heritage.module.content.entity.Article;
import com.lmf.heritage.module.content.mapper.ArticleMapper;
import com.lmf.heritage.module.content.service.ArticleLikeService;
import com.lmf.heritage.module.content.service.ArticleService;
import com.lmf.heritage.module.content.vo.ArticleDetailVO;
import com.lmf.heritage.module.content.vo.ArticleListVO;
import com.lmf.heritage.module.favorite.service.FavoriteService;
import com.lmf.heritage.module.goods.entity.Category;
import com.lmf.heritage.module.goods.mapper.CategoryMapper;
import com.lmf.heritage.security.UserContext;
import com.lmf.heritage.util.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 资讯公告服务实现
 *
 * <p><b>为什么资讯列表不整体缓存：</b>列表带 categoryId / keyword / tag / sort / 分页五组参数，
 * 组合数太多，逐组合缓存既浪费内存又容易在内容更新后留下脏页。资讯是典型的
 * 「读多写少但内容会变」的场景，交给 MySQL 的索引（idx_status_top、idx_category）足够。
 * 只有「热门资讯」这种<b>无参数</b>的榜单才适合整块缓存。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    /** 详情页相关推荐条数 */
    private static final int RELATED_LIMIT = 5;

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final ArticleLikeService articleLikeService;
    private final FavoriteService favoriteService;
    private final CacheHelper cache;

    @Override
    public PageResult<ArticleListVO> pageList(ArticleQueryDTO query) {
        query.normalize();
        Page<ArticleListVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<ArticleListVO> result = articleMapper.selectArticlePage(page, query);
        return PageResult.of(result, result.getRecords());
    }

    @Override
    public ArticleDetailVO detail(Long id) {
        Article a = articleMapper.selectById(id);
        // 草稿 / 已下架的资讯对外一律视作不存在，不泄露「这个 id 存在但没发布」
        BusinessException.throwIf(a == null
                        || !Integer.valueOf(Constants.ARTICLE_PUBLISHED).equals(a.getStatus()),
                ErrorCode.ARTICLE_NOT_FOUND);

        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setCover(a.getCover());
        vo.setSummary(a.getSummary());
        vo.setContent(a.getContent());
        vo.setAuthor(a.getAuthor());
        vo.setTags(a.getTags());
        vo.setCategoryId(a.getCategoryId());
        vo.setLikeCount(a.getLikeCount());
        vo.setCommentCount(a.getCommentCount());
        vo.setIsTop(a.getIsTop());

        // 浏览量：先打 Redis 计数器（不碰数据库），再把「库里已有的值 + 尚未回写的增量」作为展示值
        long delta = cache.increment(Constants.KEY_ARTICLE_VIEW + id);
        vo.setViewCount(nvl(a.getViewCount()) + (int) delta);

        // 分类名：详情是单条查询，这里补一次小查询即可（列表走联表，不需要）
        vo.setCategoryName(loadCategoryName(a.getCategoryId()));

        // 当前用户的点赞 / 收藏状态：未登录时为 null（前端据此不显示「已赞」态）
        Long userId = UserContext.userIdOrNull();
        if (userId != null) {
            vo.setLiked(articleLikeService.isLiked(id, userId));
            vo.setFavorited(favoriteService.isFavorited(Constants.TARGET_ARTICLE, id, userId));
        }

        vo.setCreateTime(a.getCreateTime());
        vo.setRelated(related(id, a.getCategoryId()));
        return vo;
    }

    @Override
    public List<ArticleListVO> hot(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return cache.getListOrLoad(Constants.KEY_ARTICLE_HOT + ":" + limit, ArticleListVO.class,
                Constants.TTL_ARTICLE_HOT,
                () -> articleMapper.selectHotExcluding(List.of(), limit));
    }

    @Override
    public void evictCache() {
        // 热门榜单按前缀整体失效（limit 不同 → key 不同，逐个删不现实）
        cache.deleteByPrefix(Constants.KEY_ARTICLE_HOT);
    }

    /* ==================== 内部方法 ==================== */

    /**
     * 相关推荐：先取同分类的，不够再用全站热门补齐
     *
     * <p>用 {@link LinkedHashSet} 按 id 去重 —— 补齐用的热门榜可能与同分类结果重叠，
     * 不去重就会出现同一篇资讯在「相关推荐」里出现两次。</p>
     */
    private List<ArticleListVO> related(Long id, Long categoryId) {
        List<ArticleListVO> related = new ArrayList<>();
        if (categoryId != null) {
            related.addAll(articleMapper.selectRelated(id, categoryId, RELATED_LIMIT));
        }
        if (related.size() >= RELATED_LIMIT) {
            return related;
        }

        Set<Long> exclude = new LinkedHashSet<>();
        exclude.add(id);
        for (ArticleListVO v : related) {
            exclude.add(v.getId());
        }
        List<ArticleListVO> filler = articleMapper.selectHotExcluding(
                new ArrayList<>(exclude), RELATED_LIMIT - related.size());
        related.addAll(filler);
        return related;
    }

    private String loadCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
