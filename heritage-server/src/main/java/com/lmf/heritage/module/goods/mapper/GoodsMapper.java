package com.lmf.heritage.module.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.goods.dto.AdminGoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.CraftsmanGoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsQueryDTO;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.vo.AdminGoodsDetailVO;
import com.lmf.heritage.module.goods.vo.AdminGoodsVO;
import com.lmf.heritage.module.goods.vo.CraftsmanGoodsVO;
import com.lmf.heritage.module.goods.vo.GoodsListVO;
import com.lmf.heritage.module.goods.vo.GoodsStockWarnVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 商品 Mapper
 *
 * <p>列表查询需要同时带出分类名与匠人名，用 XML 里的联表分页查询实现
 * （见 {@code resources/mapper/goods/GoodsMapper.xml}）；
 * 其余单表操作沿用 BaseMapper 的通用方法。</p>
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 前台商品列表分页查询（联表出分类名、匠人名与评价统计）
     *
     * @param page  分页对象
     * @param query 筛选条件（分类、关键词、价格区间、排序）
     */
    IPage<GoodsListVO> selectGoodsPage(IPage<GoodsListVO> page, @Param("q") GoodsQueryDTO query);

    /**
     * 把 Redis 累计的浏览量增量回写到 MySQL（定时任务用）
     *
     * @return 受影响行数
     */
    int addViewCount(@Param("id") Long id, @Param("delta") long delta);

    /** 各分类下的在售商品数，用于分类导航上的角标 */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM goods "
            + "WHERE deleted = 0 AND status = 1 AND audit_status = 1 GROUP BY category_id")
    List<Map<String, Object>> countGroupByCategory();

    /**
     * 各分类下的商品总数（不分状态）
     *
     * <p>与上面那条口径不同，用途也不同：角标要的是「有多少在卖」，
     * 删除分类前的判断要的是「这个分类是不是还挂着东西」—— 一件下架商品
     * 同样属于这个分类，不能因为下架了就允许把分类删掉。</p>
     */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM goods "
            + "WHERE deleted = 0 GROUP BY category_id")
    List<Map<String, Object>> countAllGroupByCategory();

    /** 某匠人的在售作品数 */
    @Select("SELECT COUNT(*) FROM goods WHERE deleted = 0 AND status = 1 AND audit_status = 1 "
            + "AND craftsman_id = #{craftsmanId}")
    long countByCraftsman(@Param("craftsmanId") Long craftsmanId);

    /* ==================== 匠人端商品管理（P7） ==================== */

    /**
     * 匠人端商品列表分页
     *
     * <p>与前台列表的关键差别：<b>不加 status / audit_status 的固定过滤</b>。
     * 匠人必须能看到自己待审核、已驳回、已下架的商品，否则无法处理驳回重提与重新上架。
     * 状态筛选完全交给传入条件决定。</p>
     *
     * @param page  分页对象
     * @param query 筛选条件（状态、审核状态、分类、关键词、库存预警）
     * @param craftsmanId 只查这个匠人的商品（横向隔离的落点）
     */
    IPage<CraftsmanGoodsVO> selectCraftsmanGoodsPage(IPage<CraftsmanGoodsVO> page,
                                                     @Param("craftsmanId") Long craftsmanId,
                                                     @Param("q") CraftsmanGoodsQueryDTO query);

    /**
     * 库存预警列表
     *
     * <p>口径是「在售且库存 <= 阈值」：已下架或审核未过的商品即使库存为 0 也不该报警——
     * 它本来就不在前台售卖。</p>
     */
    List<GoodsStockWarnVO> selectStockWarn(@Param("craftsmanId") Long craftsmanId,
                                           @Param("threshold") int threshold);

    /**
     * 批量上下架（只改 status，不动 audit_status）
     *
     * <p>{@code AND audit_status = 1} 写在 UPDATE 的 WHERE 里而不是由业务层预先判断：
     * 这样「只上架审核通过的商品」这条规则由数据库单条语句保证，
     * 业务层的循环校验只是为了给出更友好的错误提示，不承担正确性责任。</p>
     *
     * @return 实际生效的行数（可能与 ids.size() 不同，差额即被拦下的商品）
     */
    int batchUpdateShelf(@Param("craftsmanId") Long craftsmanId,
                         @Param("ids") List<Long> ids,
                         @Param("status") int status);

    /* ==================== 库存并发防护（P4 交易闭环） ==================== */

    /**
     * <b>扣减库存（防超卖第二层：数据库条件更新）</b>
     *
     * <p>关键在 {@code AND stock >= #{quantity}} 这个条件写在 UPDATE 的 WHERE 里：
     * InnoDB 会对该行加行锁，并在锁定后重新判断条件，因此「判断 + 扣减」是一次原子操作，
     * 天然不存在「先查后改」的竞态窗口。返回 0 行就代表库存不足，
     * 业务层直接抛异常让整个事务回滚。</p>
     *
     * <p>同时累加销量（同一事务内，保证销量与库存始终守恒）。</p>
     *
     * @param quantity 购买数量（int，调用方传 Integer 会自动拆箱）
     * @return 受影响行数，0 表示库存不足
     */
    @Update("UPDATE goods SET stock = stock - #{quantity}, sales = sales + #{quantity}, update_time = NOW() "
            + "WHERE id = #{goodsId} AND deleted = 0 AND stock >= #{quantity}")
    int deductStock(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);

    /**
     * 归还库存（取消订单 / 退款同意时调用）
     *
     * <p>销量用 {@code GREATEST(..., 0)} 兜底，避免历史数据不一致时把销量扣成负数。</p>
     */
    @Update("UPDATE goods SET stock = stock + #{quantity}, sales = GREATEST(sales - #{quantity}, 0), "
            + "update_time = NOW() WHERE id = #{goodsId} AND deleted = 0")
    int restoreStock(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);

    /* ==================== 管理端商品审核（P8） ==================== */

    /** 管理端全平台商品分页：联表带出分类名与匠人信息 */
    IPage<AdminGoodsVO> selectAdminGoodsPage(IPage<AdminGoodsVO> page,
                                             @Param("q") AdminGoodsQueryDTO query);

    /** 管理端商品审核详情：比前台详情多出匠人联系方式与全部图片 */
    AdminGoodsDetailVO selectAdminGoodsDetail(@Param("id") Long id);

    /**
     * 审核状态流转（通过 / 驳回 / 违规下架三种动作共用一条语句）
     *
     * <p>把 {@code fromAuditStatus} 写进 WHERE，使「只能流转处于预期状态的商品」成为
     * 数据库层的保证而不是业务层的约定：并发的两次审核里只有一次能改到数据，
     * 另一次影响 0 行、由调用方抛异常回滚。</p>
     *
     * <p>三种调用方式：<br>
     * 通过 —— from=0 → audit=1, status=1；<br>
     * 驳回 —— from=0 → audit=2, status=0；<br>
     * 违规下架 —— from=1 → audit=2, status=0。<br>
     * 注意下架同样把 audit_status 置为 2：只改 status 的话，
     * 匠人自己点一下「上架」就能把平台的下架决定撤销掉。</p>
     *
     * @param rejectReason 驳回 / 下架原因；通过时传空串以清掉上一轮的原因
     * @return 实际受影响行数
     */
    int updateAuditByIds(@Param("ids") List<Long> ids,
                         @Param("fromAuditStatus") int fromAuditStatus,
                         @Param("auditStatus") int auditStatus,
                         @Param("status") int status,
                         @Param("rejectReason") String rejectReason);
}
