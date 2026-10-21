package com.lmf.heritage.module.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.module.goods.dto.CategoryBatchSortDTO;
import com.lmf.heritage.module.goods.dto.CategorySaveDTO;
import com.lmf.heritage.module.goods.entity.Category;
import com.lmf.heritage.module.goods.vo.AdminCategoryVO;
import com.lmf.heritage.module.goods.vo.CategoryVO;

import java.util.List;

/**
 * 非遗分类服务
 *
 * @author 李明峰 23100025
 */
public interface CategoryService extends IService<Category> {

    /**
     * 前台分类列表（只返回启用中的，按 sort 升序）
     *
     * <p>带 {@code home:categories} 缓存，TTL 1 小时。</p>
     */
    List<CategoryVO> listEnabled();

    /** 按 id 取分类展示对象（不带缓存，详情页用） */
    CategoryVO getVO(Long id);

    /** 清分类缓存（后台增删改分类后调用） */
    void evictCache();

    /* ==================== 管理端（P8） ==================== */

    /**
     * 管理端分类列表
     *
     * <p>与 {@code listEnabled()} 的关键差别：<b>不走缓存</b>。
     * 前台那份缓存是为了扛首页流量；管理端要的是「我改完立即看到改完的样子」，
     * 一旦读到 1 小时前的快照，管理员会以为修改没生效而反复点保存。</p>
     */
    List<AdminCategoryVO> adminList();

    /**
     * 新增 / 编辑分类
     *
     * @return 分类 id
     */
    Long saveCategory(CategorySaveDTO dto);

    /** 启用 / 停用分类（停用后前台立即不可见，缓存同步清除） */
    void updateStatus(Long id, Integer status);

    /** 批量调整排序与状态，返回实际改动的条数 */
    int batchSort(CategoryBatchSortDTO dto);

    /**
     * 删除分类
     *
     * <p>分类下有商品或课程时拒绝删除（3005 / 3009）——「删除分类」的连带影响
     * 应该是「把商品一起删掉」还是「把商品变成无分类」，是个业务决策，
     * 不该由一次误点默默决定。</p>
     */
    void removeCategory(Long id);
}
