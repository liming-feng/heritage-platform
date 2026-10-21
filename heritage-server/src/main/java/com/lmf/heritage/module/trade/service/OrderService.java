package com.lmf.heritage.module.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.trade.dto.OrderBatchDeliverDTO;
import com.lmf.heritage.module.trade.dto.OrderCreateDTO;
import com.lmf.heritage.module.trade.dto.OrderDeliverDTO;
import com.lmf.heritage.module.trade.dto.OrderQueryDTO;
import com.lmf.heritage.module.trade.dto.OrderRemarkDTO;
import com.lmf.heritage.module.trade.dto.RefundDTO;
import com.lmf.heritage.module.trade.entity.Order;
import com.lmf.heritage.module.trade.vo.OrderExcelVO;
import com.lmf.heritage.module.trade.vo.OrderPreviewVO;
import com.lmf.heritage.module.trade.vo.OrderVO;

import java.util.List;
import java.util.Map;

/**
 * 订单服务
 *
 * <p>覆盖「结算预览 → 创建订单 → 支付 → 发货 → 收货 → 退款 → 评价」完整闭环。
 * 所有状态变更都必须经过 {@link com.lmf.heritage.module.trade.enums.OrderStatusEnum} 校验。</p>
 *
 * @author 李明峰 23100025
 */
public interface OrderService extends IService<Order> {

    /** 结算预览（确认订单页）：只算账不写库，提前暴露库存不足等问题 */
    OrderPreviewVO preview(OrderCreateDTO dto);

    /**
     * 创建订单
     *
     * <p>按匠人**拆单**：一次结算若包含多个匠人的作品，会生成多个订单
     * （orders 表 craftsman_id 是单一字段，拆单后匠人端分单才准确）。</p>
     *
     * @return 生成的订单列表
     */
    List<OrderVO> create(OrderCreateDTO dto);

    /** 我的订单（按状态筛选 + 分页） */
    PageResult<OrderVO> myOrders(OrderQueryDTO query);

    /** 订单详情（校验归属） */
    OrderVO detail(Long orderId);

    /** 按订单号查详情（支付回跳等场景用） */
    OrderVO detailByNo(String orderNo);

    /** 模拟支付：待付款 → 待发货 */
    void pay(String orderNo);

    /** 取消订单：仅待付款可取消，取消后归还库存 */
    void cancel(Long orderId);

    /** 确认收货：待收货 → 已完成 */
    void confirm(Long orderId);

    /** 申请退款：待发货 / 待收货 可申请 */
    void applyRefund(RefundDTO.Apply dto);

    /** 匠人端：订单列表 */
    PageResult<OrderVO> craftsmanOrders(OrderQueryDTO query);

    /** 匠人端：发货，待发货 → 待收货 */
    void deliver(OrderDeliverDTO dto);

    /** 匠人端：审核退款申请（同意 → 已退款并归还库存；驳回 → 记录原因，订单状态不变） */
    void auditRefund(RefundDTO.Audit dto);

    /** 我的订单各状态数量（tab 角标），key 为状态码，另含 all 总数 */
    Map<String, Long> myStatusCount();

    /* ==================== 匠人端订单运营（P7） ==================== */

    /**
     * 批量发货
     *
     * <p>先整批校验再整批执行：任何一单不满足条件（不是自己的、状态不是待发货），
     * 整批回滚并把有问题的订单号列在错误信息里。发货有物理后果，
     * 「有几单悄悄没发出去」比「整批重来一次」危险得多。</p>
     *
     * @return 实际发货成功的笔数
     */
    int batchDeliver(OrderBatchDeliverDTO dto);

    /** 修改匠人备注（与买家备注分开存，互不覆盖；传空串表示清空） */
    void remark(OrderRemarkDTO dto);

    /**
     * 导出订单（不分页）
     *
     * <p>导出走的是与列表同一个查询条件，所以「页面上看到的」和「导出的」是同一批数据。
     * 条数有上限（见实现里的 EXPORT_LIMIT），避免一次导出把内存和数据库都拖垮。</p>
     */
    List<OrderExcelVO> exportOrders(OrderQueryDTO query);
}
