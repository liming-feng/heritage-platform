package com.lmf.heritage.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.content.dto.AdminArticleQueryDTO;
import com.lmf.heritage.module.content.dto.ArticleQueryDTO;
import com.lmf.heritage.module.content.entity.Article;
import com.lmf.heritage.module.content.vo.AdminArticleDetailVO;
import com.lmf.heritage.module.content.vo.AdminArticleVO;
import com.lmf.heritage.module.content.vo.ArticleListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 资讯 Mapper
 *
 * <p>P3 只用它补全浏览历史的标题；P6 起承担完整的列表 / 详情 / 相关推荐查询，
 * 以及点赞数、评论数、浏览量的原子自增。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 资讯分页查询（联表带出分类名称）
     *
     * <p>只查已发布（status=1）且未删除的资讯 —— 草稿箱内容不能出现在用户端。</p>
     */
    IPage<ArticleListVO> selectArticlePage(IPage<ArticleListVO> page, @Param("q") ArticleQueryDTO q);

    /** 相关推荐：同分类、排除自己 */
    List<ArticleListVO> selectRelated(@Param("id") Long id,
                                      @Param("categoryId") Long categoryId,
                                      @Param("limit") int limit);

    /** 同分类不足时用于补齐：按浏览量取热门（排除自己与已选中的） */
    List<ArticleListVO> selectHotExcluding(@Param("excludeIds") List<Long> excludeIds,
                                           @Param("limit") int limit);

    /**
     * 管理端资讯分页
     *
     * <p>与用户端 {@link #selectArticlePage} 的区别：不过滤 status，草稿也要能查出来。
     * 分页与导出共用这一个入口，避免「列表看得见、导出少几条」。</p>
     */
    IPage<AdminArticleVO> selectAdminArticlePage(IPage<AdminArticleVO> page, @Param("q") AdminArticleQueryDTO q);

    /** 管理端资讯详情（含正文） */
    AdminArticleDetailVO selectAdminArticleDetail(@Param("id") Long id);

    /**
     * 浏览量增量回写（定时任务调用）
     *
     * <p>浏览本身只打 Redis 计数器，这里负责把累计增量刷回数据库。</p>
     */
    @Update("UPDATE article SET view_count = view_count + #{delta}, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0")
    int addViewCount(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 点赞数增减（delta 为 -1 时取消点赞）
     *
     * <p>带上 {@code like_count + #{delta} >= 0} 条件，杜绝把计数刷成负数 ——
     * 计数一旦为负，页面上的「-1 赞」很难解释。</p>
     */
    @Update("UPDATE article SET like_count = like_count + #{delta}, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0 AND like_count + #{delta} >= 0")
    int addLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /** 评论数增减，同样带非负保护 */
    @Update("UPDATE article SET comment_count = comment_count + #{delta}, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0 AND comment_count + #{delta} >= 0")
    int addCommentCount(@Param("id") Long id, @Param("delta") int delta);
}
