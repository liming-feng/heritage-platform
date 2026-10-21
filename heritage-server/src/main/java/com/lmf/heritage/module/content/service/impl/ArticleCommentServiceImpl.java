package com.lmf.heritage.module.content.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.dto.ArticleCommentDTO;
import com.lmf.heritage.module.content.entity.Article;
import com.lmf.heritage.module.content.entity.ArticleComment;
import com.lmf.heritage.module.content.mapper.ArticleCommentMapper;
import com.lmf.heritage.module.content.mapper.ArticleMapper;
import com.lmf.heritage.module.content.service.ArticleCommentService;
import com.lmf.heritage.module.content.vo.ArticleCommentVO;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资讯评论服务实现
 *
 * <p>两级评论的组装思路：<b>主楼分页 → 收集当页主楼 id → 一次查出全部回复 → 按 parentId 分组挂载</b>。
 * 共 2 次查询，与当页主楼数量无关；若换成「每个主楼查一次回复」，
 * 一页 10 条主楼就是 11 次查询。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCommentServiceImpl extends ServiceImpl<ArticleCommentMapper, ArticleComment>
        implements ArticleCommentService {

    /** 主楼 id 的默认值，表示这是一条一级评论 */
    private static final long NO_PARENT = 0L;

    private final ArticleCommentMapper commentMapper;
    private final ArticleMapper articleMapper;

    @Override
    public PageResult<ArticleCommentVO> page(Long articleId, long pageNum, long pageSize) {
        Page<ArticleCommentVO> page = new Page<>(pageNum < 1 ? 1 : pageNum,
                pageSize < 1 ? 10 : Math.min(pageSize, 50));
        IPage<ArticleCommentVO> result = commentMapper.selectTopPage(page, articleId);

        List<ArticleCommentVO> tops = result.getRecords();
        if (tops.isEmpty()) {
            return PageResult.of(result, List.of());
        }

        // 一次查出当页所有主楼下的回复，再按 parentId 分组挂上去
        List<Long> parentIds = tops.stream().map(ArticleCommentVO::getId).toList();
        List<ArticleCommentVO> replies = commentMapper.selectReplies(parentIds);

        // 主楼 id → 主楼（用于给回复补「被回复人昵称」）
        Map<Long, ArticleCommentVO> topMap = new HashMap<>();
        for (ArticleCommentVO top : tops) {
            top.setReplies(new ArrayList<>());
            topMap.put(top.getId(), top);
        }
        for (ArticleCommentVO reply : replies) {
            ArticleCommentVO parent = topMap.get(reply.getParentId());
            if (parent == null) {
                continue;
            }
            reply.setReplyToNickname(parent.getNickname());
            parent.getReplies().add(reply);
        }

        // 分页结果保持主楼顺序，这里用 LinkedHashMap 只是为了可读性（顺序无关）
        Map<Long, ArticleCommentVO> ordered = new LinkedHashMap<>(topMap);
        List<ArticleCommentVO> list = new ArrayList<>(ordered.values());
        return PageResult.of(result, list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(ArticleCommentDTO dto) {
        Long userId = UserContext.userId();

        Article article = articleMapper.selectById(dto.getArticleId());
        BusinessException.throwIf(article == null
                        || !Integer.valueOf(Constants.ARTICLE_PUBLISHED).equals(article.getStatus()),
                ErrorCode.ARTICLE_NOT_FOUND);

        long parentId = dto.getParentId() == null ? NO_PARENT : dto.getParentId();
        if (parentId != NO_PARENT) {
            ArticleComment parent = commentMapper.selectById(parentId);
            BusinessException.throwIf(parent == null, ErrorCode.PARAM_ERROR, "要回复的评论不存在");
            // 只允许回复主楼：两级结构下，「回复某条回复」在展示上仍然挂在主楼下面，
            // 因此统一要求 parentId 必须指向主楼，避免出现三层结构把树弄乱
            BusinessException.throwIf(parent.getParentId() != null && parent.getParentId() != NO_PARENT,
                    ErrorCode.PARAM_ERROR, "只支持回复主楼评论");
            BusinessException.throwIf(!dto.getArticleId().equals(parent.getArticleId()),
                    ErrorCode.PARAM_ERROR, "评论与资讯不匹配");
        }

        ArticleComment comment = new ArticleComment();
        comment.setArticleId(dto.getArticleId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        commentMapper.insert(comment);

        // 评论数 +1（带非负保护的原子自增），让详情页不用再 count 一次
        articleMapper.addCommentCount(dto.getArticleId(), 1);

        log.info("[资讯] 用户 {} 评论资讯 {}（parentId={}）", userId, dto.getArticleId(), parentId);
    }
}
