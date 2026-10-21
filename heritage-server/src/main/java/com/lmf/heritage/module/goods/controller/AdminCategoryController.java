package com.lmf.heritage.module.goods.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.goods.dto.CategoryBatchSortDTO;
import com.lmf.heritage.module.goods.dto.CategorySaveDTO;
import com.lmf.heritage.module.goods.service.CategoryService;
import com.lmf.heritage.module.goods.vo.AdminCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 · 非遗分类管理
 *
 * <p>所有写操作在服务层都会清掉 {@code home:categories} 缓存。
 * 分类是首页导航的数据源，缓存 TTL 1 小时 —— 不主动清的话，
 * 管理员新建一个分类要等一小时才在前台出现，期间会反复怀疑「是不是没保存成功」。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-分类管理", description = "分类增删改查 / 启停 / 批量排序")
@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表（管理端）",
            description = "含停用中的分类，按 sort 升序。每项带三个统计：在售商品数（前台角标口径）、"
                    + "商品总数（删除前判断）、课程数")
    @GetMapping("/list")
    public Result<List<AdminCategoryVO>> list() {
        return Result.success(categoryService.adminList());
    }

    @Operation(summary = "新增 / 编辑分类",
            description = "不传 id 为新增（不传 sort 时自动排到最后，不传 status 默认启用）；传 id 为编辑。"
                    + "分类名称全平台唯一，重名返回 3007")
    @OperationLog("分类保存")
    @PostMapping("/save")
    public Result<Long> save(@RequestBody @Valid CategorySaveDTO dto) {
        return Result.success(categoryService.saveCategory(dto));
    }

    @Operation(summary = "启用 / 停用分类",
            description = "status 0停用 / 1启用。停用后前台分类导航立即不再返回该分类")
    @OperationLog("分类启停")
    @PostMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") Integer status) {
        categoryService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "批量调整排序与状态",
            description = "单次最多 100 个。每项只需传要改的字段（sort / status 至少一个），"
                    + "返回实际改动的条数")
    @OperationLog("分类批量排序")
    @PostMapping("/batch-sort")
    public Result<Integer> batchSort(@RequestBody @Valid CategoryBatchSortDTO dto) {
        return Result.success(categoryService.batchSort(dto));
    }

    @Operation(summary = "删除分类",
            description = "分类下还有商品时返回 3005、还有课程时返回 3009。"
                    + "判断用的是商品总数（含下架与待审核），不是仅在售数")
    @OperationLog("分类删除")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        categoryService.removeCategory(id);
        return Result.success();
    }
}
