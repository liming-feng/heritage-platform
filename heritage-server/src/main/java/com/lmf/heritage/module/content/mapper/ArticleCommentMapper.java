package com.lmf.heritage.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.content.entity.ArticleComment;
import com.lmf.heritage.module.content.vo.ArticleCommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资讯评论 Mapper
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface ArticleCommentMapper extends BaseMapper<ArticleComment> {

    /**
     * 主楼分页（联表带出昵称与头像）
     *
     * <p>只查 {@code parent_id = 0} 的主楼 —— 回复挂在主楼下一起返回，
     * 不能混进分页里，否则一页 10 条可能全是回复，看不到主楼。</p>
     */
    IPage<ArticleCommentVO> selectTopPage(IPage<ArticleCommentVO> page, @Param("articleId") Long articleId);

    /**
     * 批量查这些主楼下的全部回复
     *
     * <p>一次查回来再在内存里按 parentId 分组，避免「每个主楼查一次回复」的 N+1。</p>
     */
    List<ArticleCommentVO> selectReplies(@Param("parentIds") List<Long> parentIds);
}
