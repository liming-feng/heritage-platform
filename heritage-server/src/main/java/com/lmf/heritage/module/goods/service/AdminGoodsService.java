package com.lmf.heritage.module.goods.service;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.goods.dto.AdminGoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsAuditDTO;
import com.lmf.heritage.module.goods.dto.GoodsBatchAuditDTO;
import com.lmf.heritage.module.goods.dto.GoodsOffShelfDTO;
import com.lmf.heritage.module.goods.vo.AdminGoodsDetailVO;
import com.lmf.heritage.module.goods.vo.AdminGoodsVO;
import com.lmf.heritage.module.goods.vo.GoodsExcelVO;

import java.util.List;

/**
 * 管理端 · 商品审核
 *
 * <p>三个动作的语义边界要分清：<b>审核</b>针对「待审核」的商品（通过即上架）；
 * <b>违规下架</b>针对「已通过」的商品（把审核状态一并打回，防止匠人自行重新上架）。
 * 两者允许的起始状态不同，所以是两条路径而不是一个带参数的开关。</p>
 *
 * @author 李明峰 23100025
 */
public interface AdminGoodsService {

    /** 全平台商品列表 */
    PageResult<AdminGoodsVO> page(AdminGoodsQueryDTO query);

    /** 商品审核详情 */
    AdminGoodsDetailVO auditDetail(Long id);

    /** 单件审核：通过并上架 / 驳回（驳回必填原因） */
    void audit(GoodsAuditDTO dto);

    /** 批量审核：整批回滚语义，返回生效条数 */
    int batchAudit(GoodsBatchAuditDTO dto);

    /** 违规下架：起始状态必须是「已通过」，下架后审核状态置为已驳回 */
    int offShelf(GoodsOffShelfDTO dto);

    /** 导出用：与列表同一套筛选条件 */
    List<GoodsExcelVO> exportList(AdminGoodsQueryDTO query);
}
