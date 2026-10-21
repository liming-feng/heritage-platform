package com.lmf.heritage.module.trade.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.trade.dto.OrderBatchDeliverDTO;
import com.lmf.heritage.module.trade.dto.OrderDeliverDTO;
import com.lmf.heritage.module.trade.dto.OrderQueryDTO;
import com.lmf.heritage.module.trade.dto.OrderRemarkDTO;
import com.lmf.heritage.module.trade.dto.RefundDTO;
import com.lmf.heritage.module.trade.service.OrderService;
import com.lmf.heritage.module.trade.vo.OrderExcelVO;
import com.lmf.heritage.module.trade.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 匠人端 · 订单履约
 *
 * <p>路径前缀 {@code /craftsman} 由拦截器做角色隔离：只有匠人（管理员亦可）能访问，
 * 且每个操作都会再次校验「订单是否属于当前登录匠人」，防止横向越权。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "匠人端-订单", description = "订单列表 / 发货 / 批量发货 / 备注 / 导出 / 退款审核")
@RestController
@RequestMapping("/craftsman/order")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_CRAFTSMAN, Constants.ROLE_ADMIN})
public class CraftsmanOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单列表",
            description = "refundOnly=true 只看有待处理退款申请的订单；status 不传表示全部")
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> list(OrderQueryDTO query) {
        return Result.success(orderService.craftsmanOrders(query));
    }

    @Operation(summary = "发货", description = "待发货 → 待收货，记录物流公司与单号")
    @OperationLog("订单发货")
    @PostMapping("/deliver")
    public Result<Void> deliver(@RequestBody @Valid OrderDeliverDTO dto) {
        orderService.deliver(dto);
        return Result.success();
    }

    @Operation(summary = "批量发货",
            description = "物流公司由整批共用，单号按单笔填。任何一单不合格（不属于自己 / 状态不是待发货）"
                    + "都会整批回滚，并把有问题的订单号列在错误信息里。"
                    + "单次最多 50 笔，返回实际发货笔数")
    @OperationLog("订单批量发货")
    @PostMapping("/deliver/batch")
    public Result<Integer> batchDeliver(@RequestBody @Valid OrderBatchDeliverDTO dto) {
        return Result.success(orderService.batchDeliver(dto));
    }

    @Operation(summary = "修改订单备注",
            description = "只改匠人备注，不会覆盖买家下单时填的备注。传空字符串表示清空备注")
    @OperationLog("订单备注")
    @PostMapping("/remark")
    public Result<Void> remark(@RequestBody @Valid OrderRemarkDTO dto) {
        orderService.remark(dto);
        return Result.success();
    }

    @Operation(summary = "导出订单 Excel",
            description = "查询条件与列表接口一致，导出「页面上看到的那批数据」，最多 5000 条。"
                    + "列为订单号/时间/买家/收货信息/商品明细/金额/状态/物流/备注，"
                    + "金额为可参与计算的数字而非文本")
    @GetMapping("/export")
    public void export(OrderQueryDTO query, HttpServletResponse response) throws IOException {
        List<OrderExcelVO> rows = orderService.exportOrders(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("订单列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), OrderExcelVO.class)
                .sheet("订单列表")
                .doWrite(rows);
        log.info("[订单] 导出订单列表，共 {} 条", rows.size());
    }

    @Operation(summary = "审核退款",
            description = "同意 → 订单变已退款并归还库存；驳回 → 填写驳回原因，订单状态保持不变")
    @OperationLog("退款审核")
    @PostMapping("/refund/audit")
    public Result<Void> auditRefund(@RequestBody @Valid RefundDTO.Audit dto) {
        orderService.auditRefund(dto);
        return Result.success();
    }
}
