package com.lmf.heritage.module.goods.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.mapper.CraftsmanMapper;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import com.lmf.heritage.module.goods.dto.CraftsmanGoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsSaveDTO;
import com.lmf.heritage.module.goods.dto.GoodsShelfDTO;
import com.lmf.heritage.module.goods.entity.Category;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.entity.GoodsImage;
import com.lmf.heritage.module.goods.entity.GoodsSpec;
import com.lmf.heritage.module.goods.enums.GoodsStatusEnum;
import com.lmf.heritage.module.goods.mapper.GoodsImageMapper;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.goods.mapper.GoodsSpecMapper;
import com.lmf.heritage.module.goods.service.CategoryService;
import com.lmf.heritage.module.goods.service.GoodsService;
import com.lmf.heritage.module.goods.vo.CraftsmanGoodsVO;
import com.lmf.heritage.module.goods.vo.GoodsDetailVO;
import com.lmf.heritage.module.goods.vo.GoodsListVO;
import com.lmf.heritage.module.goods.vo.GoodsSpecVO;
import com.lmf.heritage.module.goods.vo.GoodsStockWarnVO;
import com.lmf.heritage.module.goods.vo.GoodsViewTrendVO;
import com.lmf.heritage.module.review.service.ReviewService;
import com.lmf.heritage.util.CacheHelper;
import com.lmf.heritage.util.CraftsmanHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 商品服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    /** 前台可见：已上架 + 审核通过 */
    private static final int GOODS_ON = 1;
    private static final int AUDIT_PASS = 1;
    /** 详情页附带的最新评价条数 */
    private static final int DETAIL_REVIEW_LIMIT = 5;
    /** 首页热门商品条数（与缓存 key 绑定，改动时记得同步缓存说明） */
    private static final int HOT_LIMIT = 8;

    private final CategoryService categoryService;
    private final GoodsImageMapper goodsImageMapper;
    private final GoodsSpecMapper goodsSpecMapper;
    private final CraftsmanMapper craftsmanMapper;
    private final ReviewService reviewService;
    private final CacheHelper cache;
    private final CraftsmanHelper craftsmanHelper;

    @Override
    public PageResult<GoodsListVO> pageList(GoodsQueryDTO query) {
        query.normalize();
        Page<GoodsListVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<GoodsListVO> result = baseMapper.selectGoodsPage(page, query);
        log.debug("[商品] 列表查询 sort={}, categoryId={}, keyword={}, 命中 {} 条",
                query.getSort(), query.getCategoryId(), query.getKeyword(), result.getTotal());
        return PageResult.of(result, result.getRecords());
    }

    @Override
    public GoodsDetailVO detail(Long id) {
        Goods goods = getOne(Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getId, id)
                .eq(Goods::getStatus, GOODS_ON)
                .eq(Goods::getAuditStatus, AUDIT_PASS));
        BusinessException.throwIf(goods == null, ErrorCode.GOODS_NOT_FOUND);

        // 前台详情每访问一次浏览量 +1
        long delta = cache.increment(Constants.KEY_GOODS_VIEW + id);
        log.debug("[商品] 详情 id={}, 浏览量基数={}, Redis 增量={}",
                id, goods.getViewCount(), delta);
        return buildDetail(goods, delta);
    }

    @Override
    public GoodsDetailVO craftsmanDetail(Long id) {
        // 不过滤状态：待审核 / 已下架 / 已驳回的商品也必须能打开编辑
        Goods goods = getById(id);
        BusinessException.throwIf(goods == null, ErrorCode.GOODS_NOT_FOUND);
        craftsmanHelper.requireSelf(goods.getCraftsmanId());

        // 匠人自己打开编辑页不是「客户浏览」，只读计数不自增——
        // 否则匠人反复预览自己的商品会把浏览量刷得毫无参考价值
        long delta = cache.getCounter(Constants.KEY_GOODS_VIEW + id);
        return buildDetail(goods, delta);
    }

    /**
     * 组装商品详情（前台与匠人端共用）
     *
     * @param viewDelta 本次要叠加的浏览量增量：前台传「自增后的计数」，
     *                  匠人端传「当前计数」（只读，不自增）
     */
    private GoodsDetailVO buildDetail(Goods goods, long viewDelta) {
        Long id = goods.getId();
        GoodsDetailVO vo = new GoodsDetailVO();
        vo.setId(goods.getId());
        vo.setName(goods.getName());
        vo.setSubtitle(goods.getSubtitle());
        vo.setCover(goods.getCover());
        vo.setDescription(goods.getDescription());
        vo.setDetail(goods.getDetail());
        vo.setPrice(goods.getPrice());
        vo.setOriginalPrice(goods.getOriginalPrice());
        vo.setStock(goods.getStock());
        vo.setSales(goods.getSales());
        vo.setIsHandmade(goods.getIsHandmade());
        vo.setCreateTime(goods.getCreateTime());

        // 浏览量 = 库里的值 + Redis 里还没回写的增量
        int base = goods.getViewCount() == null ? 0 : goods.getViewCount();
        vo.setViewCount(base + (int) viewDelta);

        // 分类
        vo.setCategory(categoryService.getVO(goods.getCategoryId()));

        // 匠人
        Craftsman craftsman = craftsmanMapper.selectById(goods.getCraftsmanId());
        CraftsmanSimpleVO craftsmanVO = CraftsmanSimpleVO.from(craftsman);
        if (craftsmanVO != null) {
            craftsmanVO.setGoodsCount((int) baseMapper.countByCraftsman(craftsman.getId()));
        }
        vo.setCraftsman(craftsmanVO);

        // 图集：封面排第一，再拼 goods_image，去重
        LinkedHashSet<String> images = new LinkedHashSet<>();
        if (goods.getCover() != null && !goods.getCover().isBlank()) {
            images.add(goods.getCover());
        }
        List<GoodsImage> imageList = goodsImageMapper.selectList(Wrappers.<GoodsImage>lambdaQuery()
                .eq(GoodsImage::getGoodsId, id)
                .orderByAsc(GoodsImage::getSort)
                .orderByAsc(GoodsImage::getId));
        imageList.forEach(img -> {
            if (img.getUrl() != null && !img.getUrl().isBlank()) {
                images.add(img.getUrl());
            }
        });
        vo.setImages(new ArrayList<>(images));

        // 规格
        List<GoodsSpec> specs = goodsSpecMapper.selectList(Wrappers.<GoodsSpec>lambdaQuery()
                .eq(GoodsSpec::getGoodsId, id)
                .orderByAsc(GoodsSpec::getSort)
                .orderByAsc(GoodsSpec::getId));
        vo.setSpecs(specs.stream().map(s -> {
            GoodsSpecVO sv = new GoodsSpecVO();
            sv.setId(s.getId());
            sv.setSpecName(s.getSpecName());
            sv.setPrice(s.getPrice());
            sv.setStock(s.getStock());
            return sv;
        }).toList());

        // 评价统计 + 最新几条
        vo.setRatingAvg(reviewService.avgRating(Constants.REVIEW_TYPE_GOODS, id));
        vo.setReviewCount((int) reviewService.countByTarget(Constants.REVIEW_TYPE_GOODS, id));
        vo.setReviews(reviewService.listByTarget(Constants.REVIEW_TYPE_GOODS, id, DETAIL_REVIEW_LIMIT));

        return vo;
    }

    @Override
    public int totalViewsOf(Long id) {
        Goods goods = getById(id);
        BusinessException.throwIf(goods == null, ErrorCode.GOODS_NOT_FOUND);
        craftsmanHelper.requireSelf(goods.getCraftsmanId());
        return totalViews(id, goods.getViewCount());
    }

    @Override
    public GoodsViewTrendVO viewTrendOf(Long id, int days) {
        Goods goods = getById(id);
        BusinessException.throwIf(goods == null, ErrorCode.GOODS_NOT_FOUND);
        craftsmanHelper.requireSelf(goods.getCraftsmanId());

        // 天数夹在 1~90：前端传 0 或负数会让区间变成空，传 1000 会让补零循环铺满内存
        int span = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(span - 1L);

        List<Map<String, Object>> rows = baseMapper.countViewsByDay(
                Constants.TARGET_GOODS, id, from.atStartOfDay());
        Map<String, Integer> byDay = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            Object d = row.get("d");
            if (d == null) {
                continue;
            }
            Object c = row.get("c");
            // DATE() 返回 java.sql.Date，toString 即 yyyy-MM-dd，与下面补零用的 key 对齐
            byDay.put(String.valueOf(d), c == null ? 0 : ((Number) c).intValue());
        }

        List<GoodsViewTrendVO.Point> points = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate day = from.plusDays(i);
            GoodsViewTrendVO.Point p = new GoodsViewTrendVO.Point();
            p.setDate(day.toString());
            p.setDateText(day.getMonthValue() + "月" + day.getDayOfMonth() + "日");
            p.setCount(byDay.getOrDefault(day.toString(), 0));
            points.add(p);
        }

        GoodsViewTrendVO vo = new GoodsViewTrendVO();
        vo.setTotal(totalViews(id, goods.getViewCount()));
        vo.setPoints(points);
        return vo;
    }

    @Override
    public List<GoodsListVO> hotGoods(int limit) {
        int size = limit <= 0 ? HOT_LIMIT : limit;
        return cache.getListOrLoad(
                Constants.KEY_HOME_HOT_GOODS,
                GoodsListVO.class,
                Constants.TTL_HOT_GOODS,
                () -> {
                    GoodsQueryDTO q = new GoodsQueryDTO();
                    q.setSort("sales");
                    q.normalize();
                    IPage<GoodsListVO> p = baseMapper.selectGoodsPage(new Page<>(1, size, false), q);
                    log.debug("[商品] 回源热门商品 {} 条", p.getRecords().size());
                    return p.getRecords();
                });
    }

    @Override
    public long viewDelta(Long id) {
        return cache.getCounter(Constants.KEY_GOODS_VIEW + id);
    }

    @Override
    public void evictCache(Long goodsId) {
        // 「先更新数据库，再删除缓存」
        cache.delete(Constants.KEY_HOME_HOT_GOODS);
        if (goodsId != null) {
            cache.delete("goods:detail:" + goodsId);
        }
    }

    /* ==================================================================
     *  匠人端商品管理（P7）
     * ================================================================== */

    @Override
    public PageResult<CraftsmanGoodsVO> myGoods(CraftsmanGoodsQueryDTO query) {
        query.normalize();
        // 阈值只在 Constants 定义一次，这里填进去让 SQL 只负责比较
        query.setStockWarnThreshold(Constants.STOCK_WARN_THRESHOLD);

        Long craftsmanId = craftsmanHelper.currentCraftsmanId();
        Page<CraftsmanGoodsVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<CraftsmanGoodsVO> result = baseMapper.selectCraftsmanGoodsPage(page, craftsmanId, query);

        List<CraftsmanGoodsVO> records = result.getRecords();
        records.forEach(this::decorate);
        return PageResult.of(result, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveGoods(GoodsSaveDTO dto) {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        Category category = categoryService.getById(dto.getCategoryId());
        BusinessException.throwIf(category == null, ErrorCode.NOT_FOUND, "所选商品分类不存在");

        if (dto.getId() == null) {
            Goods goods = new Goods();
            goods.setCraftsmanId(craftsmanId);
            applyDto(goods, dto);
            // 发布后进入待审核：上架与否是平台审核的结论，不是匠人声明出来的
            goods.setStatus(Constants.GOODS_WAIT_AUDIT);
            goods.setAuditStatus(Constants.AUDIT_WAIT);
            goods.setSales(0);
            goods.setViewCount(0);
            save(goods);
            log.info("[商品] 匠人 {} 发布商品 {}「{}」进入待审核", craftsmanId, goods.getId(), goods.getName());
            return goods.getId();
        }

        Goods exist = getById(dto.getId());
        BusinessException.throwIf(exist == null, ErrorCode.GOODS_NOT_FOUND);
        BusinessException.throwIf(!craftsmanId.equals(exist.getCraftsmanId()),
                ErrorCode.NO_PERMISSION, "无权编辑他人的商品");

        applyDto(exist, dto);

        // 此前没过审的（待审核 / 已驳回）改稿后重新提交审核；
        // 已过审的保持原上架状态，改个价格不必再惊动平台审一遍
        boolean resubmit = !Integer.valueOf(Constants.AUDIT_PASSED).equals(exist.getAuditStatus());
        if (resubmit) {
            exist.setStatus(Constants.GOODS_WAIT_AUDIT);
            exist.setAuditStatus(Constants.AUDIT_WAIT);
        }
        updateById(exist);

        if (resubmit) {
            // updateById 默认跳过 null 字段，驳回原因必须用显式 set 才清得掉，
            // 否则重新提审后界面上还挂着上一轮的驳回理由
            baseMapper.update(null, Wrappers.<Goods>lambdaUpdate()
                    .eq(Goods::getId, exist.getId())
                    .set(Goods::getRejectReason, null));
        }

        evictCache(exist.getId());
        log.info("[商品] 匠人 {} 编辑商品 {}「{}」{}", craftsmanId, exist.getId(), exist.getName(),
                resubmit ? "并重新提交审核" : "");
        return exist.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShelf(Long id, boolean onShelf) {
        Goods goods = getById(id);
        BusinessException.throwIf(goods == null, ErrorCode.GOODS_NOT_FOUND);
        craftsmanHelper.requireSelf(goods.getCraftsmanId());

        if (onShelf) {
            // 未过审的商品不能上架，否则「发布 → 上架」这两步就绕过了平台审核
            BusinessException.throwIf(
                    !Integer.valueOf(Constants.AUDIT_PASSED).equals(goods.getAuditStatus()),
                    ErrorCode.GOODS_AUDIT_NOT_PASSED);
        }

        goods.setStatus(onShelf ? Constants.GOODS_ON_SHELF : Constants.GOODS_OFF_SHELF);
        updateById(goods);
        evictCache(id);
        log.info("[商品] 商品 {}「{}」{}", id, goods.getName(), onShelf ? "上架" : "下架");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateShelf(GoodsShelfDTO dto) {
        boolean onShelf = Boolean.TRUE.equals(dto.getOnShelf());
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        int status = onShelf ? Constants.GOODS_ON_SHELF : Constants.GOODS_OFF_SHELF;
        // 归属与「是否过审」两道条件都写在 UPDATE 的 WHERE 里：
        // 返回的 rows 就是真正被改动的条数，不需要先查一遍再逐条判断
        int rows = baseMapper.batchUpdateShelf(craftsmanId, dto.getIds(), status);

        dto.getIds().forEach(this::evictCache);
        log.info("[商品] 匠人 {} 批量{}商品，提交 {} 件，实际生效 {} 件",
                craftsmanId, onShelf ? "上架" : "下架", dto.getIds().size(), rows);
        return rows;
    }

    @Override
    public List<GoodsStockWarnVO> stockWarnList() {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();
        List<GoodsStockWarnVO> list = baseMapper.selectStockWarn(craftsmanId, Constants.STOCK_WARN_THRESHOLD);
        for (GoodsStockWarnVO vo : list) {
            int stock = vo.getStock() == null ? 0 : vo.getStock();
            // 预警等级是展示口径，放在这里算而不是存字段：
            // 阈值调整后不需要回刷历史数据
            if (stock <= 0) {
                vo.setWarnLevel("out");
                vo.setWarnText("已售罄，请尽快补货");
            } else {
                vo.setWarnLevel("low");
                vo.setWarnText("仅剩 " + stock + " 件，建议补货");
            }
        }
        return list;
    }

    /* ---------------- 内部方法 ---------------- */

    /** 把请求字段拷进实体（不含状态、销量、浏览量等由服务端决定的字段） */
    private void applyDto(Goods goods, GoodsSaveDTO dto) {
        goods.setCategoryId(dto.getCategoryId());
        goods.setName(dto.getName());
        goods.setSubtitle(dto.getSubtitle());
        goods.setCover(dto.getCover());
        goods.setDescription(dto.getDescription());
        goods.setDetail(dto.getDetail());
        goods.setPrice(dto.getPrice());
        goods.setOriginalPrice(dto.getOriginalPrice());
        goods.setStock(dto.getStock());
        goods.setIsHandmade(dto.getIsHandmade() == null ? 1 : dto.getIsHandmade());
    }

    /** 补齐列表项的派生字段：状态文案、预警标记、含未回写增量的浏览量 */
    private void decorate(CraftsmanGoodsVO vo) {
        vo.setStatusDesc(GoodsStatusEnum.descOf(vo.getStatus()));
        vo.setAuditStatusDesc(GoodsStatusEnum.Audit.descOf(vo.getAuditStatus()));
        vo.setViewCount(totalViews(vo.getId(), vo.getViewCount()));

        int stock = vo.getStock() == null ? 0 : vo.getStock();
        boolean onSale = Integer.valueOf(Constants.GOODS_ON_SHELF).equals(vo.getStatus())
                && Integer.valueOf(Constants.AUDIT_PASSED).equals(vo.getAuditStatus());
        vo.setStockWarn(onSale && stock <= Constants.STOCK_WARN_THRESHOLD);
    }

    /**
     * 累计浏览量 = 数据库已回写值 + Redis 中尚未回写的增量
     *
     * <p>浏览量是「先记 Redis、定时批量回写」的，只看数据库会少算最近一段时间的访问，
     * 匠人在后台看到的数字就会与前台详情页对不上。</p>
     */
    private int totalViews(Long goodsId, Integer dbView) {
        int base = dbView == null ? 0 : dbView;
        long delta = cache.getCounter(Constants.KEY_GOODS_VIEW + goodsId);
        return (int) (base + delta);
    }
}
