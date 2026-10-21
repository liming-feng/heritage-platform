package com.lmf.heritage.module.content.service;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.dto.AdminBannerQueryDTO;
import com.lmf.heritage.module.content.dto.BannerBatchSortDTO;
import com.lmf.heritage.module.content.dto.BannerSaveDTO;
import com.lmf.heritage.module.content.vo.AdminBannerVO;
import com.lmf.heritage.module.content.vo.BannerExcelVO;

import java.util.List;

/**
 * 管理端 · 轮播图管理
 *
 * <p>轮播图是首页的门面，写操作一律清 {@code banner:pos:*} 缓存：
 * 缓存 TTL 30 分钟，不清的话运营换完图要等半小时才生效，
 * 而这恰恰是最需要「立刻看到效果」的操作。</p>
 *
 * @author 李明峰 23100025
 */
public interface AdminBannerService {

    /** 轮播图分页（含停用） */
    PageResult<AdminBannerVO> page(AdminBannerQueryDTO query);

    /** 单张详情 */
    AdminBannerVO detail(Long id);

    /** 新增 / 编辑，返回轮播 id */
    Long save(BannerSaveDTO dto);

    /** 启用 / 停用 */
    void updateStatus(Long id, Integer status);

    /** 批量调整排序与启停，返回生效条数 */
    int batchSort(BannerBatchSortDTO dto);

    /** 删除（逻辑删除） */
    void remove(Long id);

    /** 导出：与列表同一套筛选条件 */
    List<BannerExcelVO> exportList(AdminBannerQueryDTO query);
}
