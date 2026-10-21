package com.lmf.heritage.module.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.dto.ArticleCommentDTO;
import com.lmf.heritage.module.content.entity.ArticleComment;
import com.lmf.heritage.module.content.vo.ArticleCommentVO;

/**
 * 资讯评论服务
 *
 * @author 李明峰 23100025
 */
public interface ArticleCommentService extends IService<ArticleComment> {

    /**
     * 评论分页
     *
     * <p>返回的是「主楼 + 各自回复」的两级结构：主楼分页，
     * 当页主楼的回复一次性批量查出来挂上去。</p>
     */
    PageResult<ArticleCommentVO> page(Long articleId, long pageNum, long pageSize);

    /** 发表评论或回复（同时维护 article.comment_count） */
    void publish(ArticleCommentDTO dto);
}
