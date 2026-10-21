package com.lmf.heritage.module.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.goods.dto.AdminGoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsAuditDTO;
import com.lmf.heritage.module.goods.dto.GoodsBatchAuditDTO;
import com.lmf.heritage.module.goods.dto.GoodsOffShelfDTO;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.entity.GoodsImage;
import com.lmf.heritage.module.goods.entity.GoodsSpec;
import com.lmf.heritage.module.goods.enums.GoodsStatusEnum;
import com.lmf.heritage.module.goods.mapper.GoodsImageMapper;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.goods.mapper.GoodsSpecMapper;
import com.lmf.heritage.module.goods.service.AdminGoodsService;
import com.lmf.heritage.module.goods.vo.AdminGoodsDetailVO;
import com.lmf.heritage.module.goods.vo.AdminGoodsVO;
import com.lmf.heritage.module.goods.vo.GoodsExcelVO;
import com.lmf.heritage.module.goods.vo.GoodsSpecVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端 · 商品审核实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGoodsServiceImpl implements AdminGoodsService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 导出上限，与用户 / 匠人导出一致 */
    private static final int EXPORT_LIMIT = 5000;

    /**
     * 违规下架时写在驳回原因前面的标记
     *
     * <p>{@code audit_status=2} 同一个值既表示「审核驳回」也表示「违规下架」，
     * 单看状态位分不出来。加上这个前缀，这句原因自己就说明了它是怎么来的 ——
     * 匠人看到「【违规下架】标题含极限词」比看到一句孤零零的「标题含极限词」更清楚。</p>
     */
    private static final String OFF_SHELF_TAG = "【违规下架】";

    private final GoodsMapper goodsMapper;
    private final GoodsImageMapper goodsImageMapper;
    private final GoodsSpecMapper goodsSpecMapper;

    /* ==================== 列表与详情 ==================== */

    @Override
    public PageResult<AdminGoodsVO> page(AdminGoodsQueryDTO query) {
        query.normalize();
        IPage<AdminGoodsVO> result = queryPage(query, query.getPageNum(), query.getPageSize());
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 查询 + 补齐展示字段（分页与导出共用这一个入口）
     *
     * <p>「已上架 / 待审核」这类文案不在 SQL 里算，而是由 {@link #decorate} 在 Java 端回填。
     * 导出如果另走一条「只查不补」的路，Excel 里就会出现整列空白——「页面看得见、导出看不见」
     * 正是这么来的，而且因为文件打得开、行数也对，光看导出结果很难发现。把「查」和「补」绑在
     * 同一个方法里，两条路就没有分叉的可能。</p>
     */
    private IPage<AdminGoodsVO> queryPage(AdminGoodsQueryDTO query, Long pageNum, Long pageSize) {
        Page<AdminGoodsVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminGoodsVO> result = goodsMapper.selectAdminGoodsPage(page, query);
        result.getRecords().forEach(this::decorate);
        return result;
    }

    @Override
    public AdminGoodsDetailVO auditDetail(Long id) {
        AdminGoodsDetailVO vo = goodsMapper.selectAdminGoodsDetail(id);
        BusinessException.throwIf(vo == null, ErrorCode.GOODS_NOT_FOUND);
        vo.setStatusDesc(GoodsStatusEnum.descOf(vo.getStatus()));
        vo.setAuditStatusDesc(GoodsStatusEnum.Audit.descOf(vo.getAuditStatus()));
        vo.setImages(loadImages(id));
        vo.setSpecs(loadSpecs(id));
        return vo;
    }

    private List<String> loadImages(Long goodsId) {
        // 排序与前台详情一致：sort 升序、再按 id 兜底，避免同 sort 值下顺序抖动
        return goodsImageMapper.selectList(new LambdaQueryWrapper<GoodsImage>()
                        .eq(GoodsImage::getGoodsId, goodsId)
                        .orderByAsc(GoodsImage::getSort)
                        .orderByAsc(GoodsImage::getId))
                .stream().map(GoodsImage::getUrl).toList();
    }

    private List<GoodsSpecVO> loadSpecs(Long goodsId) {
        List<GoodsSpec> list = goodsSpecMapper.selectList(new LambdaQueryWrapper<GoodsSpec>()
                .eq(GoodsSpec::getGoodsId, goodsId)
                .orderByAsc(GoodsSpec::getSort)
                .orderByAsc(GoodsSpec::getId));
        return list.stream().map(s -> {
            GoodsSpecVO v = new GoodsSpecVO();
            v.setId(s.getId());
            v.setSpecName(s.getSpecName());
            v.setPrice(s.getPrice());
            v.setStock(s.getStock());
            return v;
        }).toList();
    }

    /* ==================== 审核 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(GoodsAuditDTO dto) {
        Goods goods = goodsMapper.selectById(dto.getId());
        BusinessException.throwIf(goods == null, ErrorCode.GOODS_NOT_FOUND);

        // 只有「待审核」能被审核。放开来会出现「一件活得好好的商品被第二次审核驳回」
        // 这种无法解释的状态变化
        BusinessException.throwIf(!Integer.valueOf(Constants.AUDIT_WAIT).equals(goods.getAuditStatus()),
                ErrorCode.GOODS_AUDIT_STATUS_ERROR,
                "商品「" + goods.getName() + "」当前状态为「"
                        + GoodsStatusEnum.Audit.descOf(goods.getAuditStatus()) + "」，无需重复审核");

        boolean pass = Boolean.TRUE.equals(dto.getPass());
        String reason = trim(dto.getReason());
        BusinessException.throwIf(!pass && reason == null, ErrorCode.AUDIT_REASON_REQUIRED,
                "驳回时必须说明原因，匠人要照着改");

        int affected = goodsMapper.updateAuditByIds(List.of(goods.getId()), Constants.AUDIT_WAIT,
                pass ? Constants.AUDIT_PASSED : Constants.AUDIT_REJECTED,
                pass ? Constants.GOODS_ON_SHELF : Constants.GOODS_OFF_SHELF,
                pass ? "" : reason);
        // 走到这里还出现 0 行，说明并发下已被别人改过，抛异常让事务回滚
        BusinessException.throwIf(affected == 0, ErrorCode.GOODS_AUDIT_STATUS_ERROR);
        log.info("[商品审核]#{}（{}）{}", goods.getId(), goods.getName(), pass ? "通过并上架" : "驳回：" + reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAudit(GoodsBatchAuditDTO dto) {
        List<Long> ids = distinctIds(dto.getIds());
        boolean pass = Boolean.TRUE.equals(dto.getPass());
        String reason = trim(dto.getReason());
        BusinessException.throwIf(!pass && reason == null, ErrorCode.AUDIT_REASON_REQUIRED,
                "批量驳回时必须说明原因");

        List<Goods> goods = goodsMapper.selectBatchIds(ids);
        BusinessException.throwIf(goods.size() != ids.size(), ErrorCode.GOODS_NOT_FOUND,
                "所选商品中有 " + (ids.size() - goods.size()) + " 件不存在或已被删除");

        // 整批回滚：只要有一件不在「待审核」，整批拒绝。
        // 不做「跳过不合规的、改掉其余的」—— 那会让管理员以为整批都过完了
        List<String> bad = goods.stream()
                .filter(g -> !Integer.valueOf(Constants.AUDIT_WAIT).equals(g.getAuditStatus()))
                .map(g -> g.getName() + "（" + GoodsStatusEnum.Audit.descOf(g.getAuditStatus()) + "）")
                .toList();
        BusinessException.throwIf(!bad.isEmpty(), ErrorCode.GOODS_AUDIT_STATUS_ERROR,
                "以下商品不是待审核状态，请刷新后重试：" + String.join("、", bad));

        int affected = goodsMapper.updateAuditByIds(ids, Constants.AUDIT_WAIT,
                pass ? Constants.AUDIT_PASSED : Constants.AUDIT_REJECTED,
                pass ? Constants.GOODS_ON_SHELF : Constants.GOODS_OFF_SHELF,
                pass ? "" : reason);
        BusinessException.throwIf(affected != ids.size(), ErrorCode.GOODS_AUDIT_STATUS_ERROR,
                "实际生效 " + affected + " 件，与提交的 " + ids.size() + " 件不一致，已回滚");
        log.info("[商品批量审核] {} 件 {}", affected, pass ? "通过" : "驳回：" + reason);
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int offShelf(GoodsOffShelfDTO dto) {
        List<Long> ids = distinctIds(dto.getIds());
        String reason = trim(dto.getReason());
        BusinessException.throwIf(reason == null, ErrorCode.AUDIT_REASON_REQUIRED, "下架原因必填");

        List<Goods> goods = goodsMapper.selectBatchIds(ids);
        BusinessException.throwIf(goods.size() != ids.size(), ErrorCode.GOODS_NOT_FOUND,
                "所选商品中有 " + (ids.size() - goods.size()) + " 件不存在或已被删除");

        // 起始状态必须是「已通过」：待审核的商品走驳回，已经下架的没必要再下架一次
        List<String> bad = goods.stream()
                .filter(g -> !Integer.valueOf(Constants.AUDIT_PASSED).equals(g.getAuditStatus()))
                .map(g -> g.getName() + "（" + GoodsStatusEnum.Audit.descOf(g.getAuditStatus()) + "）")
                .toList();
        BusinessException.throwIf(!bad.isEmpty(), ErrorCode.GOODS_AUDIT_STATUS_ERROR,
                "以下商品不在「已通过」状态，无法违规下架：" + String.join("、", bad));

        int affected = goodsMapper.updateAuditByIds(ids, Constants.AUDIT_PASSED,
                Constants.AUDIT_REJECTED, Constants.GOODS_OFF_SHELF, OFF_SHELF_TAG + reason);
        BusinessException.throwIf(affected != ids.size(), ErrorCode.GOODS_AUDIT_STATUS_ERROR,
                "实际生效 " + affected + " 件，与提交的 " + ids.size() + " 件不一致，已回滚");
        log.warn("[商品违规下架] {} 件，原因：{}", affected, reason);
        return affected;
    }

    /* ==================== 导出 ==================== */

    @Override
    public List<GoodsExcelVO> exportList(AdminGoodsQueryDTO query) {
        query.normalize();
        // 与分页共用 queryPage：文案字段一并补齐，导出的就是页面上看到的那一份
        List<AdminGoodsVO> records = queryPage(query, 1L, (long) EXPORT_LIMIT).getRecords();
        return records.stream().map(v -> {
            GoodsExcelVO e = new GoodsExcelVO();
            e.setId(v.getId());
            e.setName(v.getName());
            e.setCategoryName(v.getCategoryName());
            e.setCraftsmanName(v.getCraftsmanName());
            e.setCraftType(v.getCraftType());
            e.setPrice(v.getPrice());
            e.setOriginalPrice(v.getOriginalPrice());
            e.setStock(v.getStock());
            e.setSales(v.getSales());
            e.setViewCount(v.getViewCount());
            e.setStatusDesc(v.getStatusDesc());
            e.setAuditStatusDesc(v.getAuditStatusDesc());
            e.setRejectReason(v.getRejectReason());
            e.setCreateTimeText(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== 内部工具 ==================== */

    private void decorate(AdminGoodsVO vo) {
        vo.setStatusDesc(GoodsStatusEnum.descOf(vo.getStatus()));
        vo.setAuditStatusDesc(GoodsStatusEnum.Audit.descOf(vo.getAuditStatus()));
    }

    /** 去重 + 过滤 null：前端多选组件偶尔会把 null 一起提交上来 */
    private static List<Long> distinctIds(List<Long> ids) {
        List<Long> list = ids.stream().filter(Objects::nonNull).distinct().toList();
        BusinessException.throwIf(list.isEmpty(), ErrorCode.ADMIN_OPERATION_EMPTY);
        return list;
    }

    /** 空串一律当「没填」：{@code @Size} 放得过 "   "，业务上它不是原因 */
    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
