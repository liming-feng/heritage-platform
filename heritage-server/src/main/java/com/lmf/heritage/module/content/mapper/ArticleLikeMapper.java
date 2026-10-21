package com.lmf.heritage.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.content.entity.ArticleLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资讯点赞 Mapper
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    /** 是否已点赞 */
    @Select("SELECT COUNT(*) FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int countByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /**
     * 取消点赞（物理删除）
     *
     * <p>返回受影响行数，调用方据此决定要不要把 {@code article.like_count} 减 1 ——
     * 重复点「取消」时返回 0，计数就不会被减成负数。</p>
     */
    @Delete("DELETE FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int deleteByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);
}
