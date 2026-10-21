package com.lmf.heritage.module.craftsman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.craftsman.dto.AdminCraftsmanQueryDTO;
import com.lmf.heritage.module.craftsman.dto.CraftsmanAuditDTO;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.mapper.CraftsmanMapper;
import com.lmf.heritage.module.craftsman.service.AdminCraftsmanService;
import com.lmf.heritage.module.craftsman.vo.AdminCraftsmanVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanAuditDetailVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanExcelVO;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端 · 匠人管理实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCraftsmanServiceImpl implements AdminCraftsmanService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter COURSE_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /** 导出上限，与用户导出保持一致 */
    private static final int EXPORT_LIMIT = 5000;

    /** 审核详情里的作品/课程预览条数：够看出「他确实在做这门手艺」即可，不做成第二个列表页 */
    private static final int PREVIEW_LIMIT = 5;

    private final CraftsmanMapper craftsmanMapper;
    private final GoodsMapper goodsMapper;
    private final CourseMapper courseMapper;
    private final MessageService messageService;

    /* ==================== 列表 ==================== */

    @Override
    public PageResult<AdminCraftsmanVO> page(AdminCraftsmanQueryDTO query) {
        query.normalize();
        IPage<AdminCraftsmanVO> result = queryPage(query, query.getPageNum(), query.getPageSize());
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 查询 + 补齐展示字段（分页与导出共用这一个入口）
     *
     * <p>审核状态、账号状态这些文案由 {@link #decorate} 在 Java 端回填，不在 SQL 里算。
     * 导出若另走一条「只查不补」的路，Excel 里就会出现整列空白——而文件打得开、行数也对，
     * 光看导出结果很难发现。绑成一个方法，两条路就没有分叉的可能。</p>
     */
    private IPage<AdminCraftsmanVO> queryPage(AdminCraftsmanQueryDTO query, Long pageNum, Long pageSize) {
        Page<AdminCraftsmanVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminCraftsmanVO> result = craftsmanMapper.selectAdminCraftsmanPage(page, query);
        result.getRecords().forEach(this::decorate);
        return result;
    }

    @Override
    public List<String> craftTypes() {
        return craftsmanMapper.selectCraftTypes();
    }

    @Override
    public List<CraftsmanExcelVO> exportList(AdminCraftsmanQueryDTO query) {
        query.normalize();
        // 与列表共用同一条 SQL，只是把分页放到最大 —— 保证「导出的就是筛选后的那一批」
        // 与分页共用 queryPage：文案字段一并补齐，导出的就是页面上看到的那一份
        List<AdminCraftsmanVO> rows = queryPage(query, 1L, (long) EXPORT_LIMIT).getRecords();
        return rows.stream().map(v -> {
            CraftsmanExcelVO e = new CraftsmanExcelVO();
            e.setId(v.getId());
            e.setUsername(v.getUsername());
            e.setRealName(v.getRealName());
            e.setPhone(v.getPhone());
            e.setCraftType(v.getCraftType());
            e.setInheritance(v.getInheritance());
            e.setFollowerCount(v.getFollowerCount());
            e.setGoodsCount(v.getGoodsCount());
            e.setCourseCount(v.getCourseCount());
            e.setAuditStatusText(v.getAuditStatusText());
            e.setRejectReason(v.getRejectReason());
            e.setStatusText(v.getStatusText());
            e.setCreateTimeText(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== 审核详情 ==================== */

    @Override
    public CraftsmanAuditDetailVO auditDetail(Long id) {
        CraftsmanAuditDetailVO vo = craftsmanMapper.selectAuditDetail(id);
        BusinessException.throwIf(vo == null, ErrorCode.CRAFTSMAN_NOT_FOUND);

        vo.setAuditStatusText(auditText(vo.getAuditStatus()));
        // cert_image 存的是逗号分隔的多张图（建表时按「不引第三张表」的思路定的），
        // 这里拆成数组返回，省得每个前端自己写 split 还得处理空串
        vo.setCertImages(splitImages(vo.getCertImage()));
        vo.setGoodsPreview(goodsPreview(id));
        vo.setCoursePreview(coursePreview(id));
        return vo;
    }

    /**
     * 作品预览
     *
     * <p>刻意不过滤状态：审核时「他连一件已上架的作品都没有」本身就是个判断依据，
     * 如果只查 status=1，页面上会显示成空空如也，审核员无法区分
     * 「没提交」和「提交了但都被驳回了」。</p>
     */
    private List<CraftsmanAuditDetailVO.PreviewItem> goodsPreview(Long craftsmanId) {
        List<Goods> list = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getCraftsmanId, craftsmanId)
                .orderByDesc(Goods::getId)
                .last("LIMIT " + PREVIEW_LIMIT));
        List<CraftsmanAuditDetailVO.PreviewItem> items = new ArrayList<>(list.size());
        for (Goods g : list) {
            CraftsmanAuditDetailVO.PreviewItem it = new CraftsmanAuditDetailVO.PreviewItem();
            it.setId(g.getId());
            it.setName(g.getName());
            it.setCover(g.getCover());
            it.setExtra(g.getPrice() == null ? null : "¥" + plain(g.getPrice()));
            it.setStatusText(goodsStatusText(g.getStatus(), g.getAuditStatus()));
            items.add(it);
        }
        return items;
    }

    private List<CraftsmanAuditDetailVO.PreviewItem> coursePreview(Long craftsmanId) {
        List<Course> list = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getCraftsmanId, craftsmanId)
                .orderByDesc(Course::getId)
                .last("LIMIT " + PREVIEW_LIMIT));
        List<CraftsmanAuditDetailVO.PreviewItem> items = new ArrayList<>(list.size());
        for (Course c : list) {
            CraftsmanAuditDetailVO.PreviewItem it = new CraftsmanAuditDetailVO.PreviewItem();
            it.setId(c.getId());
            it.setName(c.getTitle());
            it.setCover(c.getCover());
            it.setExtra(c.getStartTime() == null ? null : c.getStartTime().format(COURSE_TIME) + " 开课");
            it.setStatusText(courseStatusText(c.getStatus(), c.getAuditStatus()));
            items.add(it);
        }
        return items;
    }

    /* ==================== 审核 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(CraftsmanAuditDTO dto) {
        Craftsman craftsman = craftsmanMapper.selectById(dto.getId());
        BusinessException.throwIf(craftsman == null, ErrorCode.CRAFTSMAN_NOT_FOUND);

        // 幂等护栏：已通过 / 已驳回的不允许再改。
        // 放开来会引出「驳回后匠人拒不修改、反复点通过」的扯皮场景，
        // 真要改先做「撤销审核」这个显式动作（后续阶段再补）。
        BusinessException.throwIf(!Integer.valueOf(Constants.AUDIT_WAIT).equals(craftsman.getAuditStatus()),
                ErrorCode.CRAFTSMAN_AUDIT_STATUS_ERROR);

        boolean pass = Boolean.TRUE.equals(dto.getPass());
        String reason = dto.getReason() == null ? null : dto.getReason().trim();
        BusinessException.throwIf(!pass && (reason == null || reason.isEmpty()),
                ErrorCode.AUDIT_REASON_REQUIRED, "驳回时必须说明原因，申请人要照着改");

        Craftsman update = new Craftsman();
        update.setId(craftsman.getId());
        update.setAuditStatus(pass ? Constants.AUDIT_PASSED : Constants.AUDIT_REJECTED);
        // 通过时把驳回原因清空：这个字段在详情页是直接展示的，
        // 留着上一次的驳回理由会让人以为「通过了但有问题」
        update.setRejectReason(pass ? "" : reason);
        update.setAuditTime(LocalDateTime.now());
        craftsmanMapper.updateById(update);

        notify(craftsman.getUserId(), craftsman.getId(), pass, reason,
                craftsman.getRealName(), craftsman.getCraftType());
        log.info("[匠人审核]#{} {} {}", craftsman.getId(), pass ? "通过" : "驳回",
                pass ? "" : "原因：" + reason);
    }

    /**
     * 审核结果通知申请人
     *
     * <p>走 {@code MessageService}，它内部已做异常兜底 —— 消息发不出去不该让审核回滚：
     * 审核结论是管理动作，通知只是善后。</p>
     */
    private void notify(Long userId, Long craftsmanId, boolean pass, String reason,
                        String realName, String craftType) {
        if (userId == null) {
            return;
        }
        String who = (realName == null || realName.isBlank()) ? "您" : realName;
        if (pass) {
            messageService.send(userId, Constants.MSG_SYSTEM, "匠人资质审核已通过",
                    "恭喜！" + who + "的" + (craftType == null ? "" : craftType)
                            + "匠人资质已通过平台审核，现在可以发布作品、开设课程并接待预约了。",
                    craftsmanId);
        } else {
            messageService.send(userId, Constants.MSG_SYSTEM, "匠人资质审核未通过",
                    "很抱歉，" + who + "提交的匠人资质未通过审核。原因：" + reason
                            + "。请补充材料后重新提交。",
                    craftsmanId);
        }
    }

    /* ==================== 内部工具 ==================== */

    private void decorate(AdminCraftsmanVO vo) {
        vo.setAuditStatusText(auditText(vo.getAuditStatus()));
        vo.setStatusText(Integer.valueOf(Constants.STATUS_ENABLED).equals(vo.getStatus()) ? "正常" : "禁用");
    }

    private static List<String> splitImages(String certImage) {
        if (certImage == null || certImage.isBlank()) {
            return List.of();
        }
        return Arrays.stream(certImage.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    static String auditText(Integer s) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case Constants.AUDIT_WAIT -> "待审核";
            case Constants.AUDIT_PASSED -> "已通过";
            case Constants.AUDIT_REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    private static String goodsStatusText(Integer status, Integer auditStatus) {
        // 审核状态优先：一件「待审核」的商品说它「已下架」是误导
        if (Integer.valueOf(Constants.AUDIT_REJECTED).equals(auditStatus)) {
            return "已驳回";
        }
        if (Integer.valueOf(Constants.AUDIT_WAIT).equals(auditStatus)) {
            return "待审核";
        }
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case Constants.GOODS_ON_SHELF -> "在售";
            case Constants.GOODS_OFF_SHELF -> "已下架";
            default -> "待审核";
        };
    }

    private static String courseStatusText(Integer status, Integer auditStatus) {
        if (Integer.valueOf(Constants.AUDIT_REJECTED).equals(auditStatus)) {
            return "已驳回";
        }
        if (Integer.valueOf(Constants.AUDIT_WAIT).equals(auditStatus)) {
            return "待审核";
        }
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case Constants.COURSE_NOT_START -> "未开课";
            case Constants.COURSE_ONGOING -> "进行中";
            case Constants.COURSE_ENDED -> "已结束";
            default -> "未知";
        };
    }

    /** 去掉 BigDecimal 尾零：¥120.00 在审核卡片上不如 ¥120 好认 */
    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
