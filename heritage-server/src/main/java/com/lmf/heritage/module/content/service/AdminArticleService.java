package com.lmf.heritage.module.content.service;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.dto.AdminArticleQueryDTO;
import com.lmf.heritage.module.content.dto.ArticleBatchStatusDTO;
import com.lmf.heritage.module.content.dto.ArticleSaveDTO;
import com.lmf.heritage.module.content.vo.AdminArticleDetailVO;
import com.lmf.heritage.module.content.vo.AdminArticleVO;
import com.lmf.heritage.module.content.vo.ArticleExcelVO;

import java.util.List;

/**
 * 管理端 · 资讯内容管理
 *
 * <p>这里的「发布 / 下架」不是审核：资讯由平台自己撰写，没有第三方提交审核这层关系，
 * 因此 {@code status} 是一个可以直接来回扳的开关（1 已发布 / 0 草稿或下架），
 * 不需要像商品那样记录「谁审的、为什么驳回」。</p>
 *
 * <p>所有写操作都会清掉资讯相关缓存（热门资讯、首页聚合），
 * 否则发布一篇新资讯后前台要等缓存 TTL 才看得到。</p>
 *
 * @author 李明峰 23100025
 */
public interface AdminArticleService {

    /** 资讯列表（含草稿与已下架） */
    PageResult<AdminArticleVO> page(AdminArticleQueryDTO query);

    /** 资讯详情（含正文，编辑页回显用） */
    AdminArticleDetailVO detail(Long id);

    /** 新增 / 编辑资讯，返回资讯 id */
    Long save(ArticleSaveDTO dto);

    /** 发布 / 下架单篇 */
    void updateStatus(Long id, Integer status);

    /** 批量发布 / 下架，返回生效条数（幂等：对已处于目标状态的资讯可重复执行） */
    int batchStatus(ArticleBatchStatusDTO dto);

    /** 置顶 / 取消置顶 */
    void updateTop(Long id, Integer isTop);

    /** 删除资讯（逻辑删除） */
    void remove(Long id);

    /** 导出：与列表同一套筛选条件 */
    List<ArticleExcelVO> exportList(AdminArticleQueryDTO query);
}
