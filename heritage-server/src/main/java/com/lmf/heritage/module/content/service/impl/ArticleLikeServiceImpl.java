package com.lmf.heritage.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.content.entity.Article;
import com.lmf.heritage.module.content.entity.ArticleLike;
import com.lmf.heritage.module.content.mapper.ArticleLikeMapper;
import com.lmf.heritage.module.content.mapper.ArticleMapper;
import com.lmf.heritage.module.content.service.ArticleLikeService;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资讯点赞服务实现
 *
 * <p><b>点赞的两道防线（与商品库存、预约名额是同一套思路）：</b></p>
 * <ol>
 *   <li>先查一次「是否已点赞」——挡住绝大多数重复点击，且能返回明确提示；</li>
 *   <li>数据库唯一键 {@code uk_user_article} —— 并发下两次请求同时通过第 1 步时，
 *       只有一个 insert 能成功，另一个抛 {@link DuplicateKeyException}，被翻译成同样的提示。</li>
 * </ol>
 *
 * <p>计数与点赞记录在同一个事务里：不会出现「记录插进去了但 like_count 没加」——
 * 那会表现为「明明赞了，数字没变」，用户会一直点。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike>
        implements ArticleLikeService {

    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleMapper articleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long articleId) {
        Long userId = UserContext.userId();
        Article article = articleMapper.selectById(articleId);
        BusinessException.throwIf(article == null
                        || !Integer.valueOf(Constants.ARTICLE_PUBLISHED).equals(article.getStatus()),
                ErrorCode.ARTICLE_NOT_FOUND);

        // 第一道：可读性更好的快速判断
        BusinessException.throwIf(articleLikeMapper.countByUserAndArticle(userId, articleId) > 0,
                ErrorCode.REPEAT_SUBMIT, "你已经点过赞了");

        ArticleLike like = new ArticleLike();
        like.setUserId(userId);
        like.setArticleId(articleId);
        try {
            articleLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            // 第二道：并发下唯一键兜底。这里不抛原始异常，给用户同一句话
            log.debug("[资讯] 并发点赞被唯一键拦下 userId={} articleId={}", userId, articleId);
            throw new BusinessException(ErrorCode.REPEAT_SUBMIT, "你已经点过赞了");
        }

        articleMapper.addLikeCount(articleId, 1);
        log.debug("[资讯] 用户 {} 点赞资讯 {}", userId, articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long articleId) {
        Long userId = UserContext.userId();

        // 删除返回 0 行说明本来就没赞过 —— 此时不能减计数，否则计数会变成负数
        int rows = articleLikeMapper.deleteByUserAndArticle(userId, articleId);
        if (rows == 0) {
            log.debug("[资讯] 用户 {} 未点赞过资讯 {}，取消操作忽略", userId, articleId);
            return;
        }
        articleMapper.addLikeCount(articleId, -1);
        log.debug("[资讯] 用户 {} 取消点赞资讯 {}", userId, articleId);
    }

    @Override
    public boolean isLiked(Long articleId, Long userId) {
        if (userId == null || articleId == null) {
            return false;
        }
        return articleLikeMapper.countByUserAndArticle(userId, articleId) > 0;
    }
}
