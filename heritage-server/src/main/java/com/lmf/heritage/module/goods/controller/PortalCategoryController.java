package com.lmf.heritage.module.goods.controller;

import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.goods.service.CategoryService;
import com.lmf.heritage.module.goods.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 · 非遗分类
 *
 * <p>路径不含 /api：context-path 已经补上。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-分类", description = "非遗分类导航")
@RestController
@RequestMapping("/portal/category")
@RequiredArgsConstructor
public class PortalCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表", description = "只返回启用中的分类，按 sort 升序；带 home:categories 缓存（1 小时）")
    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.listEnabled());
    }
}
