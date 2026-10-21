package com.lmf.heritage.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.dto.AdminArticleQueryDTO;
import com.lmf.heritage.module.content.dto.ArticleBatchStatusDTO;
import com.lmf.heritage.module.content.dto.ArticleSaveDTO;
import com.lmf.heritage.module.content.entity.Article;
import com.lmf.heritage.module.content.mapper.ArticleMapper;
import com.lmf.heritage.module.content.service.AdminArticleService;
import com.lmf.heritage.module.content.service.ArticleService;
import com.lmf.heritage.module.content.vo.AdminArticleDetailVO;
import com.lmf.heritage.module.content.vo.AdminArticleVO;
import com.lmf.heritage.module.content.vo.ArticleExcelVO;
import com.lmf.heritage.module.goods.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端 · 资讯内容管理实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminArticleServiceImpl implements AdminArticleService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 导出上限，与用户 / 商品导出一致 */
    private static final int EXPORT_LIMIT = 5000;

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final ArticleService articleService;

    /* ==================== 列表与详情 ==================== */

    @Override
    public PageResult<AdminArticleVO> page(AdminArticleQueryDTO query) {
        query.normalize();
        IPage<AdminArticleVO> result = queryPage(query, query.getPageNum(), query.getPageSize());
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 查询 + 补齐展示字段（分页与导出共用）
     *
     * <p>「已发布 / 草稿」这类文案由 {@link #decorate} 在 Java 端回填，不在 SQL 里拼。
     * 导出与分页走的是同一个方法，因此不会出现「页面上写着已发布、导出来是空」——
     * 这个坑在 P8 前四个模块里出现过一次，靠结构而不是靠记性来避免复发。</p>
     */
    private IPage<AdminArticleVO> queryPage(AdminArticleQueryDTO query, Long pageNum, Long pageSize) {
        Page<AdminArticleVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminArticleVO> result = articleMapper.selectAdminArticlePage(page, query);
        result.getRecords().forEach(this::decorate);
        return result;
    }

    @Override
    public AdminArticleDetailVO detail(Long id) {
        AdminArticleDetailVO vo = articleMapper.selectAdminArticleDetail(id);
        BusinessException.throwIf(vo == null, ErrorCode.ARTICLE_NOT_FOUND);
        decorate(vo);
        return vo;
    }

    /* ==================== 新增 / 编辑 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(ArticleSaveDTO dto) {
        dto.normalize();

        // 分类是外键，先查一次：不校验的话填错 id 会存进一条指向不存在的分类的资讯，
        // 列表页 classification 列就一直空着，而且没人知道为什么
        if (dto.getCategoryId() != null) {
            BusinessException.throwIf(categoryMapper.selectById(dto.getCategoryId()) == null,
                    ErrorCode.CATEGORY_NOT_FOUND);
        }

        boolean isNew = dto.getId() == null;
        Article article;
        if (isNew) {
            article = new Article();
            article.setViewCount(0);
            article.setLikeCount(0);
            article.setCommentCount(0);
            // 新增默认值：草稿、不置顶。想直接发布请显式传 status=1
            article.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.ARTICLE_OFFLINE);
            article.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : 0);
        } else {
            article = articleMapper.selectById(dto.getId());
            BusinessException.throwIf(article == null, ErrorCode.ARTICLE_NOT_FOUND);
            if (dto.getStatus() != null) {
                article.setStatus(dto.getStatus());
            }
            if (dto.getIsTop() != null) {
                article.setIsTop(dto.getIsTop());
            }
        }

        article.setTitle(dto.getTitle());
        article.setCategoryId(dto.getCategoryId());
        article.setCover(dto.getCover());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setAuthor(dto.getAuthor());
        article.setTags(dto.getTags());

        if (isNew) {
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
        }

        articleService.evictCache();
        log.info("[资讯管理] {}资讯 #{}（{}）status={} isTop={}",
                isNew ? "新增" : "编辑", article.getId(), article.getTitle(),
                article.getStatus(), article.getIsTop());
        return article.getId();
    }

    /* ==================== 状态流转 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireValidStatus(status);
        Article article = requireArticle(id);

        article.setStatus(status);
        articleMapper.updateById(article);
        articleService.evictCache();
        log.info("[资讯管理] 资讯 #{} 状态改为 {}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchStatus(ArticleBatchStatusDTO dto) {
        requireValidStatus(dto.getStatus());
        List<Long> ids = dto.getIds().stream().filter(Objects::nonNull).distinct().toList();
        BusinessException.throwIf(ids.isEmpty(), ErrorCode.ADMIN_OPERATION_EMPTY);

        int affected = articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .in(Article::getId, ids)
                .set(Article::getStatus, dto.getStatus()));
        articleService.evictCache();
        log.info("[资讯管理] 批量改状态为 {}，提交 {} 条，生效 {} 条", dto.getStatus(), ids.size(), affected);
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTop(Long id, Integer isTop) {
        BusinessException.throwIf(isTop == null || (isTop != 0 && isTop != 1),
                ErrorCode.PARAM_ERROR, "isTop 只能是 0（取消置顶）或 1（置顶）");
        Article article = requireArticle(id);

        article.setIsTop(isTop);
        articleMapper.updateById(article);
        articleService.evictCache();
        log.info("[资讯管理] 资讯 #{} {}", id, isTop == 1 ? "置顶" : "取消置顶");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Article article = requireArticle(id);
        // 逻辑删除：@TableLogic 会把 deleteById 改写成 UPDATE deleted=1，
        // 评论与点赞仍挂在 article_id 上，物理删会让这些记录变成孤儿
        articleMapper.deleteById(id);
        articleService.evictCache();
        log.warn("[资讯管理] 删除资讯 #{}（{}）", id, article.getTitle());
    }

    /* ==================== 导出 ==================== */

    @Override
    public List<ArticleExcelVO> exportList(AdminArticleQueryDTO query) {
        query.normalize();
        List<AdminArticleVO> records = queryPage(query, 1L, (long) EXPORT_LIMIT).getRecords();
        return records.stream().map(v -> {
            ArticleExcelVO e = new ArticleExcelVO();
            e.setId(v.getId());
            e.setTitle(v.getTitle());
            e.setCategoryName(v.getCategoryName());
            e.setAuthor(v.getAuthor());
            e.setTags(v.getTags());
            e.setStatusDesc(v.getStatusDesc());
            e.setIsTopDesc(v.getIsTopDesc());
            e.setViewCount(v.getViewCount());
            e.setLikeCount(v.getLikeCount());
            e.setCommentCount(v.getCommentCount());
            e.setCreateTimeText(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== 内部工具 ==================== */

    private void decorate(AdminArticleVO vo) {
        vo.setStatusDesc(Integer.valueOf(Constants.ARTICLE_PUBLISHED).equals(vo.getStatus()) ? "已发布" : "草稿");
        vo.setIsTopDesc(Integer.valueOf(1).equals(vo.getIsTop()) ? "置顶" : "—");
    }

    private Article requireArticle(Long id) {
        Article article = articleMapper.selectById(id);
        BusinessException.throwIf(article == null, ErrorCode.ARTICLE_NOT_FOUND);
        return article;
    }

    private static void requireValidStatus(Integer status) {
        BusinessException.throwIf(status == null
                        || (status != Constants.ARTICLE_OFFLINE && status != Constants.ARTICLE_PUBLISHED),
                ErrorCode.PARAM_ERROR, "status 只能是 0（草稿/下架）或 1（已发布）");
    }
}
