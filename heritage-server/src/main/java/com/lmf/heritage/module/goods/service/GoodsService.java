package com.lmf.heritage.module.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.goods.dto.CraftsmanGoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsSaveDTO;
import com.lmf.heritage.module.goods.dto.GoodsShelfDTO;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.vo.CraftsmanGoodsVO;
import com.lmf.heritage.module.goods.vo.GoodsDetailVO;
import com.lmf.heritage.module.goods.vo.GoodsListVO;
import com.lmf.heritage.module.goods.vo.GoodsStockWarnVO;
import com.lmf.heritage.module.goods.vo.GoodsViewTrendVO;

import java.util.List;

/**
 * 商品服务
 *
 * @author 李明峰 23100025
 */
public interface GoodsService extends IService<Goods> {

    /**
     * 前台商品列表（分类筛选 / 关键词搜索 / 价格区间 / 排序 / 分页）
     *
     * <p>走自定义联表查询，一次性带出分类名、匠人名与评价统计。</p>
     */
    PageResult<GoodsListVO> pageList(GoodsQueryDTO query);

    /**
     * 商品详情：基本信息 + 图集 + 规格 + 匠人简介 + 评价列表
     *
     * <p>同时会把 Redis 浏览量计数器 +1，并叠加到返回的 viewCount 上。</p>
     */
    GoodsDetailVO detail(Long id);

    /** 首页热门商品（按销量），带 {@code home:hotGoods} 缓存 */
    List<GoodsListVO> hotGoods(int limit);

    /** 当前 Redis 中该商品未回写的浏览量增量 */
    long viewDelta(Long id);

    /** 清商品相关缓存（商品变更后调用） */
    void evictCache(Long goodsId);

    /* ==================== 匠人端商品管理（P7） ==================== */

    /**
     * 我的商品列表
     *
     * <p>不做上架/审核过滤——匠人要能看到自己全部状态的作品才能处理驳回重提与重新上架。
     * 只圈定 {@code craftsman_id = 当前登录匠人}，越权在 SQL 层就被挡住。</p>
     */
    PageResult<CraftsmanGoodsVO> myGoods(CraftsmanGoodsQueryDTO query);

    /**
     * 发布 / 编辑商品
     *
     * <p>发布：状态置为待审核，等管理端放行。<br>
     * 编辑：若商品此前未通过审核（待审核或已驳回），改稿后重新回到待审核；
     * 已通过审核的商品编辑后保持原上架状态，不必再审一次。</p>
     *
     * @return 商品 id
     */
    Long saveGoods(GoodsSaveDTO dto);

    /**
     * 单个上下架
     *
     * <p>上架的前提是审核通过：{@code audit_status = 1}。待审核 / 已驳回的商品
     * 不能直接上架，否则「先发布再上架」就绕过了审核。</p>
     */
    void updateShelf(Long id, boolean onShelf);

    /**
     * 批量上下架
     *
     * <p>返回<b>实际生效的条数</b>，而不是「提交的条数」：
     * 其中不满足上架条件（未过审、不属于自己）的商品会被数据库语句自身的条件挡下，
     * 调用方拿到的数字即真实处理量。</p>
     */
    int batchUpdateShelf(GoodsShelfDTO dto);

    /** 库存预警列表（在售 + 审核通过 + 库存 <= 阈值） */
    List<GoodsStockWarnVO> stockWarnList();

    /**
     * 匠人端的商品详情（编辑回显）
     *
     * <p>与前台 {@link #detail(Long)} 的两点差别：<br>
     * ① 不过滤上架与审核状态——待审核 / 已下架 / 已驳回的商品也要能打开编辑；<br>
     * ② <b>不增加浏览量</b>。匠人自己预览不是客户浏览，计入会污染这个指标。</p>
     */
    GoodsDetailVO craftsmanDetail(Long id);

    /** 商品累计浏览量（数据库已回写值 + Redis 未回写增量），带归属校验 */
    int totalViewsOf(Long id);

    /**
     * 浏览量趋势：累计浏览量 + 最近 {@code days} 天按天补零的浏览点
     *
     * <p>只允许商品所属匠人（或管理员）查看，防横向越权。</p>
     *
     * @param days 天数，1~90，超出会被夹到边界
     */
    GoodsViewTrendVO viewTrendOf(Long id, int days);
}
