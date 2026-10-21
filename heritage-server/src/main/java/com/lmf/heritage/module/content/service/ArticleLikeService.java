package com.lmf.heritage.module.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.module.content.entity.ArticleLike;

/**
 * 资讯点赞服务
 *
 * @author 李明峰 23100025
 */
public interface ArticleLikeService extends IService<ArticleLike> {

    /** 点赞：一人一篇只能点一次，成功后 {@code article.like_count} +1 */
    void like(Long articleId);

    /** 取消点赞：物理删掉点赞记录并把计数 -1（重复调用只生效一次） */
    void unlike(Long articleId);

    /** 是否已点赞；未登录返回 false */
    boolean isLiked(Long articleId, Long userId);
}
