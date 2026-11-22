package com.lmf.heritage.module.goods.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.goods.dto.CraftsmanGoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsSaveDTO;
import com.lmf.heritage.module.goods.dto.GoodsShelfDTO;
import com.lmf.heritage.module.goods.service.GoodsService;
import com.lmf.heritage.module.goods.vo.CraftsmanGoodsVO;
import com.lmf.heritage.module.goods.vo.GoodsDetailVO;
import com.lmf.heritage.module.goods.vo.GoodsStockWarnVO;
import com.lmf.heritage.module.goods.vo.GoodsViewTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 匠人端 · 商品管理
 *
 * <p>路径前缀 {@code /craftsman} 由拦截器做角色隔离；服务层再按
 * {@code craftsman_id = 当前登录匠人} 圈定数据范围，防止横向越权改到别人的商品。</p>
 *
 * <p><b>发布为什么不能直接上架</b>：发布接口不接收状态字段，新商品一律进「待审核」，
 * 由管理端审核通过后才允许上架。否则匠人只要在发布时声明 status=1，
 * 平台审核这一环就被绕过去了。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "匠人端-商品", description = "我的商品 / 发布编辑 / 上下架 / 库存预警")
@RestController
@RequestMapping("/craftsman/goods")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_CRAFTSMAN, Constants.ROLE_ADMIN})
public class CraftsmanGoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "我的商品",
            description = "只看自己的商品，含下架 / 待审核 / 已驳回等全部状态。"
                    + "支持 status、auditStatus、categoryId、keyword、stockWarn（只看库存预警）、"
                    + "sort（default/sales/stock/view）与分页")
    @GetMapping("/list")
    public Result<PageResult<CraftsmanGoodsVO>> list(CraftsmanGoodsQueryDTO query) {
        return Result.success(goodsService.myGoods(query));
    }

    @Operation(summary = "商品详情（编辑回显）",
            description = "匠人查看自己的商品；前台详情只返回已上架且过审的商品，"
                    + "待审核 / 已下架的商品必须能在这里看到才能编辑")
    @GetMapping("/{id}")
    public Result<GoodsDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(goodsService.craftsmanDetail(id));
    }

    @Operation(summary = "发布 / 编辑商品",
            description = "不传 id 为发布（进入待审核）；传 id 为编辑。"
                    + "编辑时若该商品此前未过审，改稿后自动重新提交审核并清空上一轮驳回原因")
    @OperationLog("发布/编辑商品")
    @PostMapping("/save")
    public Result<Long> save(@RequestBody @Valid GoodsSaveDTO dto) {
        return Result.success(goodsService.saveGoods(dto));
    }

    @Operation(summary = "上架 / 下架",
            description = "onShelf=true 上架，false 下架。上架要求商品已通过平台审核，"
                    + "否则返回 3006")
    @OperationLog("商品上下架")
    @PostMapping("/{id}/shelf")
    public Result<Void> shelf(@PathVariable("id") Long id,
                             @RequestParam("onShelf") Boolean onShelf) {
        goodsService.updateShelf(id, Boolean.TRUE.equals(onShelf));
        return Result.success();
    }

    @Operation(summary = "批量上架 / 下架",
            description = "单次最多 50 件。返回实际生效条数——未过审或不属于自己的商品会被挡下，"
                    + "因此生效数可能小于提交数")
    @OperationLog("商品批量上下架")
    @PostMapping("/shelf/batch")
    public Result<Integer> batchShelf(@RequestBody @Valid GoodsShelfDTO dto) {
        return Result.success(goodsService.batchUpdateShelf(dto));
    }

    @Operation(summary = "库存预警列表",
            description = "在售且已过审、库存 <= 5 的商品。库存为 0 标记为 out（已售罄），"
                    + "其余标记为 low（库存告急）")
    @GetMapping("/stock-warn")
    public Result<List<GoodsStockWarnVO>> stockWarn() {
        return Result.success(goodsService.stockWarnList());
    }

    @Operation(summary = "浏览量",
            description = "返回数据库已回写值 + Redis 未回写增量，"
                    + "保证与前台详情页显示的累计访问量一致")
    @GetMapping("/{id}/views")
    public Result<Integer> views(@PathVariable("id") Long id) {
        return Result.success(goodsService.totalViewsOf(id));
    }

    @Operation(summary = "浏览量趋势",
            description = "返回累计浏览量 + 最近 days 天按天补零的浏览点。"
                    + "点数据来自 browse_history（有行为记录的登录用户），"
                    + "因此其求和与累计访问量不相等——累计量含未登录等请求")
    @GetMapping("/{id}/view-trend")
    public Result<GoodsViewTrendVO> viewTrend(@PathVariable("id") Long id,
                                              @RequestParam(value = "days", defaultValue = "7") Integer days) {
        return Result.success(goodsService.viewTrendOf(id, days == null ? 7 : days));
    }
}
