package com.lmf.heritage.module.craftsman.service;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.craftsman.dto.AdminCraftsmanQueryDTO;
import com.lmf.heritage.module.craftsman.dto.CraftsmanAuditDTO;
import com.lmf.heritage.module.craftsman.vo.AdminCraftsmanVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanAuditDetailVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanExcelVO;

import java.util.List;

/**
 * 管理端 · 匠人管理（含入驻资质审核）
 *
 * @author 李明峰 23100025
 */
public interface AdminCraftsmanService {

    /** 匠人列表（资质状态 / 门类 / 账号状态 / 关键词 / 申请时间筛选） */
    PageResult<AdminCraftsmanVO> page(AdminCraftsmanQueryDTO query);

    /** 资质审核详情（唯一返回证件号与资质图的出口） */
    CraftsmanAuditDetailVO auditDetail(Long id);

    /**
     * 资质审核
     *
     * <p>通过 → 状态置「已通过」，同时清空历史驳回原因（否则重新通过后页面上还挂着上次的驳回理由）；
     * 驳回 → 必填原因。两种结果都会给申请人发一条站内消息。</p>
     */
    void audit(CraftsmanAuditDTO dto);

    /** 门类下拉选项（取自现有数据） */
    List<String> craftTypes();

    /** 导出用：与列表同一套筛选条件 */
    List<CraftsmanExcelVO> exportList(AdminCraftsmanQueryDTO query);
}
