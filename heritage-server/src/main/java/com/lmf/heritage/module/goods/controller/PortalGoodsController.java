package com.lmf.heritage.module.goods.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.goods.dto.GoodsQueryDTO;
import com.lmf.heritage.module.goods.service.GoodsService;
import com.lmf.heritage.module.goods.vo.GoodsDetailVO;
import com.lmf.heritage.module.goods.vo.GoodsListVO;
import com.lmf.heritage.module.user.service.HistoryService;
import com.lmf.heritage.module.user.vo.HistoryGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 · 商品浏览
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-商品", description = "商品列表 / 搜索 / 详情 / 浏览历史")
@RestController
@RequestMapping("/portal/goods")
@RequiredArgsConstructor
public class PortalGoodsController {

    private final GoodsService goodsService;
    private final HistoryService historyService;

    @Operation(summary = "商品列表",
            description = "支持分类筛选 categoryId、关键词 keyword、价格区间 minPrice/maxPrice、"
                    + "是否纯手工 isHandmade、排序 sort（default/sales/price_asc/price_desc/new）、分页")
    @GetMapping("/list")
    public Result<PageResult<GoodsListVO>> list(GoodsQueryDTO query) {
        return Result.success(goodsService.pageList(query));
    }

    @Operation(summary = "商品搜索", description = "关键词必填，其余参数同列表接口")
    @GetMapping("/search")
    public Result<PageResult<GoodsListVO>> search(GoodsQueryDTO query) {
        BusinessException.throwIf(query.getKeyword() == null || query.getKeyword().isBlank(),
                ErrorCode.PARAM_ERROR, "请输入搜索关键词");
        return Result.success(goodsService.pageList(query));
    }

    @Operation(summary = "商品详情",
            description = "基本信息 + 图集 + 规格 + 匠人简介 + 评价列表；同时 Redis 浏览量计数器 +1")
    @GetMapping("/{id}")
    public Result<GoodsDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(goodsService.detail(id));
    }

    @Operation(summary = "商品浏览历史", description = "等同于 /portal/history/list?targetType=1，需登录")
    @LoginRequired
    @GetMapping("/history")
    public Result<List<HistoryGroupVO>> history() {
        return Result.success(historyService.listGrouped(Constants.TARGET_GOODS));
    }
}
