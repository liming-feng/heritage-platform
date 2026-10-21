package com.lmf.heritage.module.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.dto.ArticleQueryDTO;
import com.lmf.heritage.module.content.entity.Article;
import com.lmf.heritage.module.content.vo.ArticleDetailVO;
import com.lmf.heritage.module.content.vo.ArticleListVO;

import java.util.List;

/**
 * 资讯公告服务
 *
 * @author 李明峰 23100025
 */
public interface ArticleService extends IService<Article> {

    /** 资讯分页（只返回已发布内容） */
    PageResult<ArticleListVO> pageList(ArticleQueryDTO query);

    /**
     * 资讯详情
     *
     * <p>副作用：把 Redis 里的浏览量计数器 +1（不写库），
     * 并把当前计数叠加到返回的 viewCount 上，保证用户刷新就能看到自己这一次。</p>
     */
    ArticleDetailVO detail(Long id);

    /** 热门资讯（按浏览量，走缓存） */
    List<ArticleListVO> hot(int limit);

    /** 清除资讯相关缓存 */
    void evictCache();
}
