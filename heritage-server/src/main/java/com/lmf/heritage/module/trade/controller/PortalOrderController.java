package com.lmf.heritage.module.trade.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.RateLimit;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.trade.dto.OrderCreateDTO;
import com.lmf.heritage.module.trade.dto.OrderQueryDTO;
import com.lmf.heritage.module.trade.dto.RefundDTO;
import com.lmf.heritage.module.trade.service.OrderService;
import com.lmf.heritage.module.trade.vo.OrderPreviewVO;
import com.lmf.heritage.module.trade.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户端 · 订单
 *
 * <p>完整闭环：结算预览 → 创建订单 → 模拟支付 → 确认收货 → 申请退款。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-订单", description = "结算 / 下单 / 支付 / 取消 / 收货 / 退款")
@RestController
@RequestMapping("/portal/order")
@RequiredArgsConstructor
@LoginRequired
public class PortalOrderController {

    private final OrderService orderService;

    @Operation(summary = "结算预览",
            description = "确认订单页用：不写库，只算账并校验库存。"
                    + "items 不传表示结算购物车勾选项，传了表示立即购买")
    @PostMapping("/preview")
    public Result<OrderPreviewVO> preview(@RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.preview(dto));
    }

    @Operation(summary = "创建订单",
            description = "按匠人自动拆单，返回生成的订单列表；"
                    + "库存扣减有 Redis 锁 + 数据库条件更新两层保护，不会超卖")
    @RateLimit(count = 40, window = 60, dimension = "user", message = "下单太频繁了，请稍后再试")
    @PostMapping("/create")
    public Result<List<OrderVO>> create(@RequestBody @Valid OrderCreateDTO dto) {
        return Result.success(orderService.create(dto));
    }

    @Operation(summary = "我的订单", description = "status 不传表示全部；orderNo 支持模糊搜索")
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> list(OrderQueryDTO query) {
        return Result.success(orderService.myOrders(query));
    }

    @Operation(summary = "各状态订单数量", description = "返回 {状态码: 数量, all: 总数}，用于 tab 角标")
    @GetMapping("/count")
    public Result<Map<String, Long>> count() {
        return Result.success(orderService.myStatusCount());
    }

    @Operation(summary = "订单详情")
    @GetMapping("/detail/{id}")
    public Result<OrderVO> detail(@PathVariable("id") Long id) {
        return Result.success(orderService.detail(id));
    }

    @Operation(summary = "按订单号查详情", description = "支付回跳等场景用")
    @GetMapping("/detail-no/{orderNo}")
    public Result<OrderVO> detailByNo(@PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.detailByNo(orderNo));
    }

    @Operation(summary = "模拟支付", description = "待付款 → 待发货，记录支付时间")
    @PostMapping("/pay/{orderNo}")
    public Result<Void> pay(@PathVariable("orderNo") String orderNo) {
        orderService.pay(orderNo);
        return Result.success();
    }

    @Operation(summary = "取消订单", description = "仅待付款可取消，取消后归还库存")
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    @Operation(summary = "确认收货", description = "待收货 → 已完成")
    @PostMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable("id") Long id) {
        orderService.confirm(id);
        return Result.success();
    }

    @Operation(summary = "申请退款", description = "待发货 / 待收货可申请，需等待匠人审核")
    @PostMapping("/refund/apply")
    public Result<Void> applyRefund(@RequestBody @Valid RefundDTO.Apply dto) {
        orderService.applyRefund(dto);
        return Result.success();
    }
}
