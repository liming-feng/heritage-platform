package com.lmf.heritage.module.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.coupon.service.CouponService;
import com.lmf.heritage.module.coupon.vo.CouponUseResultVO;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.entity.GoodsSpec;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.goods.mapper.GoodsSpecMapper;
import com.lmf.heritage.module.trade.dto.OrderBatchDeliverDTO;
import com.lmf.heritage.module.trade.dto.OrderCreateDTO;
import com.lmf.heritage.module.trade.dto.OrderDeliverDTO;
import com.lmf.heritage.module.trade.dto.OrderQueryDTO;
import com.lmf.heritage.module.trade.dto.OrderRemarkDTO;
import com.lmf.heritage.module.trade.dto.RefundDTO;
import com.lmf.heritage.module.trade.entity.Address;
import com.lmf.heritage.module.trade.entity.Order;
import com.lmf.heritage.module.trade.entity.OrderItem;
import com.lmf.heritage.module.trade.enums.OrderStatusEnum;
import com.lmf.heritage.module.trade.mapper.OrderItemMapper;
import com.lmf.heritage.module.trade.mapper.OrderMapper;
import com.lmf.heritage.module.trade.service.AddressService;
import com.lmf.heritage.module.trade.service.CartService;
import com.lmf.heritage.module.trade.service.OrderService;
import com.lmf.heritage.module.trade.vo.OrderExcelVO;
import com.lmf.heritage.module.trade.vo.OrderItemVO;
import com.lmf.heritage.module.trade.vo.OrderPreviewVO;
import com.lmf.heritage.module.trade.vo.OrderVO;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.mapper.UserMapper;
import com.lmf.heritage.security.UserContext;
import com.lmf.heritage.util.CraftsmanHelper;
import com.lmf.heritage.util.RedisLockUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsSpecMapper goodsSpecMapper;
    private final AddressService addressService;
    private final CartService cartService;
    private final CraftsmanHelper craftsmanHelper;
    private final UserMapper userMapper;
    private final RedisLockUtil redisLockUtil;
    private final ObjectMapper objectMapper;
    private final CouponService couponService;

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** 导出 Excel 的时间列格式：日期时间都要，便于对账时直接排序 */
    private static final DateTimeFormatter EXPORT_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 单次导出的最大行数：导出不分页，必须有上限兜住内存与查询耗时 */
    private static final int EXPORT_LIMIT = 5000;
    /** 库存扣减的 Redis 锁持有时间（毫秒），到期自动释放，避免宕机后死锁 */
    private static final long STOCK_LOCK_LEASE_MILLIS = 5_000L;
    /** 库存扣减的抢锁等待时间（毫秒），等不到才放弃 */
    private static final long STOCK_LOCK_WAIT_MILLIS = 2_000L;

    /* ==================================================================
     *  结算预览
     * ================================================================== */

    @Override
    public OrderPreviewVO preview(OrderCreateDTO dto) {
        Long userId = UserContext.userId();
        List<PendingItem> pendings = collectPendingItems(dto);

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setAddress(dto.getAddressId() != null
                ? addressService.requireOwned(dto.getAddressId(), userId)
                : addressService.getDefault());

        // 预览时再检查一次库存：购物车列表页停留久了，库存可能已被别人买走
        List<String> problems = new ArrayList<>();
        List<com.lmf.heritage.module.trade.vo.CartItemVO> items = new ArrayList<>();
        for (PendingItem p : pendings) {
            com.lmf.heritage.module.trade.vo.CartItemVO item = new com.lmf.heritage.module.trade.vo.CartItemVO();
            item.setGoodsId(p.getGoodsId());
            item.setSpecId(p.getSpecId());
            item.setQuantity(p.getQuantity());
            item.setGoodsName(p.getGoodsName());
            item.setCover(p.getCover());
            item.setSpecName(p.getSpecName());
            item.setPrice(p.getPrice());
            item.setStock(p.getStock());
            item.setCraftsmanId(p.getCraftsmanId());
            item.setInvalid(p.getStock() < p.getQuantity() ? 1 : 0);
            item.calcSubtotal();
            items.add(item);
            if (item.getInvalid() == 1) {
                problems.add("「" + p.getGoodsName() + "」库存不足，仅剩 " + p.getStock() + " 件");
            }
        }

        BigDecimal total = items.stream()
                .map(i -> i.getSubtotal() == null ? BigDecimal.ZERO : i.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        vo.setItems(items);
        vo.setTotalAmount(total);

        // 选了券就试算一次抵扣（只算不改：这里绝不能把券标记成已使用）
        BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (dto.getUserCouponId() != null) {
            discount = couponService.previewDiscount(dto.getUserCouponId(), total, Constants.TARGET_GOODS);
        }
        vo.setDiscountAmount(discount);
        vo.setUserCouponId(dto.getUserCouponId());
        vo.setPayAmount(total.subtract(discount).setScale(2, RoundingMode.HALF_UP));
        vo.setTotalQuantity(items.stream().mapToInt(i -> nvl(i.getQuantity())).sum());
        vo.setCanSubmit(!items.isEmpty() && problems.isEmpty());
        if (items.isEmpty()) {
            vo.setMessage("没有可结算的商品，请先勾选购物车商品或选择立即购买");
        } else if (!problems.isEmpty()) {
            vo.setMessage(String.join("；", problems));
        }
        return vo;
    }

    /* ==================================================================
     *  创建订单
     * ================================================================== */

    /**
     * 创建订单（事务）
     *
     * <p>整体顺序：校验地址 → 归集商品 → 按匠人拆单 → 逐单扣库存 → 落库 → 清购物车。
     * 任何一步抛异常都会回滚整个事务，因此不会出现「库存扣了但订单没生成」的中间态。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> create(OrderCreateDTO dto) {
        Long userId = UserContext.userId();
        Address address = addressService.requireOwned(dto.getAddressId(), userId);
        List<PendingItem> pendings = collectPendingItems(dto);
        BusinessException.throwIf(pendings.isEmpty(), ErrorCode.CART_EMPTY);

        boolean fromCart = dto.getItems() == null || dto.getItems().isEmpty();

        // 按匠人拆单：orders.craftsman_id 是单一字段，一单只能属于一个匠人
        Map<Long, List<PendingItem>> grouped = pendings.stream()
                .collect(Collectors.groupingBy(PendingItem::getCraftsmanId,
                        LinkedHashMap::new, Collectors.toList()));

        // 拆单与优惠券互斥：一张券只属于一笔订单，拆成多单后「抵扣额怎么分摊、某单退款后
        // 券怎么归还」都变得有歧义（部分退款后券还了，另一单却已经享受过折扣）。
        // 与其写一套模糊的分摊规则，不如直接要求用户分开结算——规则简单且没有漏洞。
        BusinessException.throwIf(dto.getUserCouponId() != null && grouped.size() > 1,
                ErrorCode.COUPON_NOT_AVAILABLE,
                "本次结算包含多个匠人的商品，请分开下单后再使用优惠券");

        List<OrderVO> result = new ArrayList<>(grouped.size());
        for (Map.Entry<Long, List<PendingItem>> entry : grouped.entrySet()) {
            Long craftsmanId = entry.getKey();
            List<PendingItem> group = entry.getValue();

            // ① 扣库存：先扣后建单，扣不动就抛异常回滚（不会产生超卖订单）
            for (PendingItem item : group) {
                deductStockWithLock(item);
            }

            // ② 订单主表
            BigDecimal total = group.stream()
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            Order order = new Order();
            order.setOrderNo(generateOrderNo(userId));
            order.setUserId(userId);
            order.setCraftsmanId(craftsmanId);
            order.setTotalAmount(total);

            // ③ 用券：条件更新把券置为已使用，同时拿到券种 id 与抵扣金额
            BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (dto.getUserCouponId() != null) {
                CouponUseResultVO used = couponService.useForOrder(
                        dto.getUserCouponId(), total, Constants.TARGET_GOODS, order.getOrderNo());
                discount = used.getDiscountAmount();
                order.setCouponId(used.getCouponId());
                order.setUserCouponId(used.getUserCouponId());
            }
            order.setDiscountAmount(discount);
            order.setPayAmount(total.subtract(discount).setScale(2, RoundingMode.HALF_UP));
            order.setStatus(Constants.ORDER_WAIT_PAY);
            order.setRefundStatus(0);
            order.setReceiver(address.getReceiver());
            order.setReceiverPhone(address.getPhone());
            order.setAddressSnapshot(buildAddressSnapshot(address));
            order.setRemark(dto.getRemark());
            orderMapper.insert(order);

            // ④ 订单项（商品名/图片/规格名/单价全部快照）
            List<OrderItem> items = new ArrayList<>(group.size());
            for (PendingItem p : group) {
                OrderItem item = new OrderItem();
                item.setOrderId(order.getId());
                item.setOrderNo(order.getOrderNo());
                item.setGoodsId(p.getGoodsId());
                item.setSpecId(p.getSpecId());
                item.setGoodsName(p.getGoodsName());
                item.setGoodsImage(p.getCover());
                item.setSpecName(p.getSpecName());
                item.setPrice(p.getPrice());
                item.setQuantity(p.getQuantity());
                item.setReviewed(0);
                orderItemMapper.insert(item);
                items.add(item);
            }

            result.add(assembleOrder(order, items, null));
            log.info("[订单] 用户 {} 下单成功 orderNo={} 匠人 {} 金额 {}",
                    userId, order.getOrderNo(), craftsmanId, total);
        }

        // ⑤ 购物车下单成功后清掉已勾选项（同一事务，失败一起回滚）
        if (fromCart) {
            cartService.removeChecked();
        }
        return result;
    }

    /**
     * <b>扣减库存 —— 并发防超卖的两层保护</b>
     *
     * <pre>
     * 第一层：Redis 分布式锁 lock:goods:{goodsId}
     *        作用是削峰与串行化：把同一商品的并发请求排队，减少数据库行锁等待
     *        与连接占用。最多等 2 秒，等不到才抛「正在抢购，请稍后重试」，
     *        避免请求长时间挂住（压测 20 并发时，15 个请求都能排到锁、
     *        再在第二层被库存不足挡下，而不是在锁上就被拒）。
     *
     * 第二层：数据库条件更新 UPDATE ... WHERE stock >= #{quantity}
     *        这才是**正确性**的保证。判断与扣减在一条 SQL 内完成并持有行锁，
     *        不存在「先查库存再扣」的竞态窗口；返回 0 行即库存不足。
     * </pre>
     *
     * <p>锁在 finally 中释放（Lua 脚本比对 requestId，不会误删别人的锁）。
     * 锁的持有时间短于事务，因此后到的请求即使拿到锁，
     * 它的 UPDATE 也会阻塞在前一个事务的行锁上，顺序执行、结果依然正确。</p>
     */
    private void deductStockWithLock(PendingItem item) {
        String lockKey = Constants.KEY_GOODS_LOCK + item.getGoodsId();
        String requestId = UUID.randomUUID().toString();

        // 短暂自旋等待（2 秒）后再失败：让并发请求排队依次进入扣减逻辑，
        // 把「谁能买到」交给数据库的条件更新去裁决，而不是由锁直接拒掉大部分请求；
        // 等不到锁才快速失败，避免请求长时间挂住。
        if (!redisLockUtil.lock(lockKey, requestId, STOCK_LOCK_WAIT_MILLIS, STOCK_LOCK_LEASE_MILLIS)) {
            throw new BusinessException(ErrorCode.REPEAT_SUBMIT, "「" + item.getGoodsName() + "」正在被抢购，请稍后重试");
        }
        try {
            // 规格库存与商品总库存同时扣：规格决定能不能买，商品库存是总量上限
            if (item.getSpecId() != null) {
                int specRows = goodsSpecMapper.deductStock(item.getSpecId(), item.getQuantity());
                BusinessException.throwIf(specRows == 0, ErrorCode.STOCK_NOT_ENOUGH,
                        "「" + item.getGoodsName() + "-" + item.getSpecName() + "」库存不足");
            }
            int rows = goodsMapper.deductStock(item.getGoodsId(), item.getQuantity());
            BusinessException.throwIf(rows == 0, ErrorCode.STOCK_NOT_ENOUGH,
                    "「" + item.getGoodsName() + "」库存不足");
        } finally {
            redisLockUtil.unlock(lockKey, requestId);
        }
    }

    /** 归还库存（取消订单 / 退款同意） */
    private void restoreStock(OrderItem item) {
        if (item.getSpecId() != null) {
            goodsSpecMapper.restoreStock(item.getSpecId(), item.getQuantity());
        }
        goodsMapper.restoreStock(item.getGoodsId(), item.getQuantity());
    }

    /* ==================================================================
     *  查询
     * ================================================================== */

    @Override
    public PageResult<OrderVO> myOrders(OrderQueryDTO query) {
        query.normalize();
        Long userId = UserContext.userId();

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getOrderNo()), Order::getOrderNo, query.getOrderNo())
                .orderByDesc(Order::getId);

        Page<Order> page = orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page, assembleList(page.getRecords(), false));
    }

    @Override
    public OrderVO detail(Long orderId) {
        Long userId = UserContext.userId();
        Order order = orderMapper.selectById(orderId);
        BusinessException.throwIf(order == null, ErrorCode.ORDER_NOT_FOUND);
        BusinessException.throwIf(!userId.equals(order.getUserId()), ErrorCode.ORDER_NOT_BELONG_TO_YOU);

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        return assembleOrder(order, items, null);
    }

    @Override
    public OrderVO detailByNo(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo).last("LIMIT 1"));
        BusinessException.throwIf(order == null, ErrorCode.ORDER_NOT_FOUND);
        return detail(order.getId());
    }

    @Override
    public PageResult<OrderVO> craftsmanOrders(OrderQueryDTO query) {
        query.normalize();
        Long craftsmanId = currentCraftsmanId();

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getCraftsmanId, craftsmanId)
                .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getOrderNo()), Order::getOrderNo, query.getOrderNo())
                // 只看待处理的退款申请
                .eq(Boolean.TRUE.equals(query.getRefundOnly()), Order::getRefundStatus, 1)
                .orderByDesc(Order::getId);

        Page<Order> page = orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page, assembleList(page.getRecords(), true));
    }

    @Override
    public Map<String, Long> myStatusCount() {
        Long userId = UserContext.userId();
        Map<String, Long> map = new LinkedHashMap<>();
        long all = 0L;
        for (Map<String, Object> row : orderMapper.countGroupByStatusForUser(userId)) {
            int status = ((Number) row.get("status")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            map.put(String.valueOf(status), cnt);
            all += cnt;
        }
        map.put("all", all);
        return map;
    }

    /* ==================================================================
     *  状态流转
     * ================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(String orderNo) {
        Order order = requireMineByNo(orderNo);
        OrderStatusEnum.checkTransfer(order.getStatus(), OrderStatusEnum.WAIT_DELIVER);

        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.WAIT_DELIVER.getCode());
        update.setPayTime(LocalDateTime.now());
        // 模拟支付：真实项目这里应该调用支付网关，回调后再改状态
        orderMapper.updateById(update);
        log.info("[订单] 支付成功 orderNo={} 金额={}", orderNo, order.getPayAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        Order order = requireMine(orderId);
        OrderStatusEnum.checkTransfer(order.getStatus(), OrderStatusEnum.CANCELED);

        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.CANCELED.getCode());
        orderMapper.updateById(update);

        // 取消后归还库存（未支付订单同样占了库存，必须还回去）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        items.forEach(this::restoreStock);

        // 归还优惠券：券是按「未支付也先占住」处理的，订单取消必须还回去，
        // 否则用户会平白损失一张券（这一点与库存归还同等重要）
        couponService.giveBackByOrderNo(order.getOrderNo());

        log.info("[订单] 取消订单 orderNo={} 归还 {} 个商品项库存", order.getOrderNo(), items.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        Order order = requireMine(orderId);
        OrderStatusEnum.checkTransfer(order.getStatus(), OrderStatusEnum.FINISHED);

        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.FINISHED.getCode());
        update.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(update);
        log.info("[订单] 确认收货 orderNo={}", order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(RefundDTO.Apply dto) {
        Order order = requireMine(dto.getOrderId());
        // 只有「已付款且未完成」的订单能申请退款；待付款直接取消即可
        boolean refundable = Integer.valueOf(Constants.ORDER_WAIT_DELIVER).equals(order.getStatus())
                || Integer.valueOf(Constants.ORDER_WAIT_RECEIVE).equals(order.getStatus());
        BusinessException.throwIf(!refundable,
                ErrorCode.ORDER_STATUS_ERROR, "当前订单状态不支持申请退款");
        BusinessException.throwIf(Integer.valueOf(1).equals(order.getRefundStatus()),
                ErrorCode.ORDER_STATUS_ERROR, "退款申请已提交，请等待匠人处理");

        Order update = new Order();
        update.setId(order.getId());
        update.setRefundStatus(1);
        update.setRefundReason(dto.getReason());
        update.setRefundRejectReason(null);
        orderMapper.updateById(update);
        log.info("[订单] 申请退款 orderNo={} 原因={}", order.getOrderNo(), dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(OrderDeliverDTO dto) {
        Order order = requireCraftsmanOrder(dto.getOrderId());
        OrderStatusEnum.checkTransfer(order.getStatus(), OrderStatusEnum.WAIT_RECEIVE);

        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.WAIT_RECEIVE.getCode());
        update.setLogisticsCompany(dto.getLogisticsCompany());
        update.setLogisticsNo(dto.getLogisticsNo());
        update.setCraftsmanRemark(dto.getCraftsmanRemark());
        update.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(update);
        log.info("[订单] 匠人发货 orderNo={} 物流={} {}", order.getOrderNo(),
                dto.getLogisticsCompany(), dto.getLogisticsNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(RefundDTO.Audit dto) {
        Order order = requireCraftsmanOrder(dto.getOrderId());
        BusinessException.throwIf(!Integer.valueOf(1).equals(order.getRefundStatus()),
                ErrorCode.ORDER_STATUS_ERROR, "该订单没有待处理的退款申请");

        if (Boolean.TRUE.equals(dto.getApprove())) {
            OrderStatusEnum.checkTransfer(order.getStatus(), OrderStatusEnum.REFUNDED);
            Order update = new Order();
            update.setId(order.getId());
            update.setStatus(OrderStatusEnum.REFUNDED.getCode());
            update.setRefundStatus(2);
            orderMapper.updateById(update);

            // 同意退款 → 归还库存，并把用掉的优惠券也还给用户
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            items.forEach(this::restoreStock);
            couponService.giveBackByOrderNo(order.getOrderNo());
            log.info("[订单] 同意退款 orderNo={} 已归还库存与优惠券", order.getOrderNo());
        } else {
            BusinessException.throwIf(!StringUtils.hasText(dto.getRejectReason()),
                    ErrorCode.PARAM_ERROR, "驳回退款时请填写驳回原因");
            Order update = new Order();
            update.setId(order.getId());
            update.setRefundStatus(3);
            update.setRefundRejectReason(dto.getRejectReason());
            // 订单状态保持不变：商品照常履约
            orderMapper.updateById(update);
            log.info("[订单] 驳回退款 orderNo={} 原因={}", order.getOrderNo(), dto.getRejectReason());
        }
    }

    /* ==================================================================
     *  内部方法
     * ================================================================== */

    /**
     * 归集待下单项：优先用请求里传的商品（立即购买），否则取购物车勾选项。
     * 同商品同规格会被合并数量，避免重复项。
     */
    private List<PendingItem> collectPendingItems(OrderCreateDTO dto) {
        List<PendingItem> list = new ArrayList<>();

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (OrderCreateDTO.Item item : dto.getItems()) {
                list.add(loadPendingItem(item.getGoodsId(), item.getSpecId(), item.getQuantity()));
            }
        } else {
            List<com.lmf.heritage.module.trade.vo.CartItemVO> checked = cartService.listCheckedItems();
            BusinessException.throwIf(checked.isEmpty(), ErrorCode.CART_EMPTY);
            for (com.lmf.heritage.module.trade.vo.CartItemVO c : checked) {
                list.add(loadPendingItem(c.getGoodsId(), c.getSpecId(), c.getQuantity()));
            }
        }

        // 合并同商品同规格
        Map<String, PendingItem> merged = new LinkedHashMap<>();
        for (PendingItem p : list) {
            String key = p.getGoodsId() + ":" + p.getSpecId();
            PendingItem exist = merged.get(key);
            if (exist == null) {
                merged.put(key, p);
            } else {
                exist.setQuantity(exist.getQuantity() + p.getQuantity());
                // 合并后库存要按最新数量重新判定
                exist.setStock(p.getStock());
            }
        }
        return new ArrayList<>(merged.values());
    }

    /** 查商品实时信息并组装待下单项（下单前的最后一道商品校验） */
    private PendingItem loadPendingItem(Long goodsId, Long specId, Integer quantity) {
        BusinessException.throwIf(quantity == null || quantity < 1, ErrorCode.PARAM_ERROR, "购买数量不合法");
        Goods goods = goodsMapper.selectById(goodsId);
        BusinessException.throwIf(goods == null, ErrorCode.GOODS_NOT_FOUND);
        BusinessException.throwIf(nvl(goods.getStatus()) != 1 || nvl(goods.getAuditStatus()) != 1,
                ErrorCode.GOODS_OFF_SHELF, "「" + goods.getName() + "」已下架");

        GoodsSpec spec = null;
        if (specId != null) {
            spec = goodsSpecMapper.selectById(specId);
            BusinessException.throwIf(spec == null || !goods.getId().equals(spec.getGoodsId()),
                    ErrorCode.PARAM_ERROR, "所选规格不存在");
        }

        PendingItem p = new PendingItem();
        p.setGoodsId(goods.getId());
        p.setCraftsmanId(goods.getCraftsmanId());
        p.setGoodsName(goods.getName());
        p.setCover(goods.getCover());
        p.setSpecId(specId);
        p.setSpecName(spec == null ? null : spec.getSpecName());
        p.setPrice(spec == null ? goods.getPrice() : spec.getPrice());
        p.setStock(spec == null ? nvl(goods.getStock()) : nvl(spec.getStock()));
        p.setQuantity(quantity);
        return p;
    }

    /** 生成订单号：HO + 时间(14) + 用户后四位 + 6 位随机 */
    private String generateOrderNo(Long userId) {
        String random = String.format("%06d", (int) (Math.random() * 1_000_000));
        return "HO" + ORDER_NO_FORMATTER.format(LocalDateTime.now())
                + String.format("%04d", userId % 10000) + random;
    }

    /** 收货地址快照（JSON）；序列化失败时降级为拼接文本，绝不让快照影响下单 */
    private String buildAddressSnapshot(Address address) {
        try {
            return objectMapper.writeValueAsString(address);
        } catch (Exception e) {
            log.warn("[订单] 地址快照序列化失败，降级为文本: {}", e.getMessage());
            return String.join(" ", nvlStr(address.getProvince()), nvlStr(address.getCity()),
                    nvlStr(address.getDistrict()), nvlStr(address.getDetail())).trim();
        }
    }

    /** 批量组装订单视图（一次查完订单项与买家，避免 N+1） */
    private List<OrderVO> assembleList(List<Order> orders, boolean withBuyer) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.listByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        Map<Long, String> buyerMap = new HashMap<>();
        if (withBuyer) {
            List<Long> userIds = orders.stream().map(Order::getUserId).distinct().toList();
            List<User> users = userMapper.selectBatchIds(userIds);
            buyerMap = users.stream().collect(Collectors.toMap(
                    User::getId, u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername()));
        }

        List<OrderVO> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            result.add(assembleOrder(order, itemMap.getOrDefault(order.getId(), List.of()),
                    buyerMap.get(order.getUserId())));
        }
        return result;
    }

    /** 单条订单组装（含前端按钮开关） */
    private OrderVO assembleOrder(Order order, List<OrderItem> items, String buyerName) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusDesc(OrderStatusEnum.of(order.getStatus()).getDesc());
        vo.setBuyerName(buyerName);

        List<OrderItemVO> itemVOs = new ArrayList<>(items.size());
        for (OrderItem item : items) {
            OrderItemVO iv = new OrderItemVO();
            BeanUtils.copyProperties(item, iv);
            iv.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP));
            itemVOs.add(iv);
        }
        vo.setItems(itemVOs);
        vo.setTotalQuantity(itemVOs.stream().mapToInt(i -> nvl(i.getQuantity())).sum());

        // 按钮开关统一由后端算好，前端不用再写一遍状态判断
        int status = nvl(order.getStatus());
        boolean refundApplying = Integer.valueOf(1).equals(order.getRefundStatus());
        vo.setCanPay(status == Constants.ORDER_WAIT_PAY);
        vo.setCanCancel(status == Constants.ORDER_WAIT_PAY);
        vo.setCanRefund((status == Constants.ORDER_WAIT_DELIVER || status == Constants.ORDER_WAIT_RECEIVE)
                && !refundApplying);
        vo.setCanConfirm(status == Constants.ORDER_WAIT_RECEIVE);
        vo.setCanReview(status == Constants.ORDER_FINISHED
                && itemVOs.stream().anyMatch(i -> nvl(i.getReviewed()) == 0));
        vo.setCanDeliver(status == Constants.ORDER_WAIT_DELIVER);
        vo.setCanAuditRefund(refundApplying);
        return vo;
    }

    /** 取本人订单，不存在抛订单不存在，不属于本人抛无权操作 */
    private Order requireMine(Long orderId) {
        Long userId = UserContext.userId();
        Order order = orderMapper.selectById(orderId);
        BusinessException.throwIf(order == null, ErrorCode.ORDER_NOT_FOUND);
        BusinessException.throwIf(!userId.equals(order.getUserId()), ErrorCode.ORDER_NOT_BELONG_TO_YOU);
        return order;
    }

    private Order requireMineByNo(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo).last("LIMIT 1"));
        BusinessException.throwIf(order == null, ErrorCode.ORDER_NOT_FOUND);
        BusinessException.throwIf(!UserContext.userId().equals(order.getUserId()),
                ErrorCode.ORDER_NOT_BELONG_TO_YOU);
        return order;
    }

    /** 匠人端操作：订单必须属于当前登录用户绑定的匠人 */
    /* ==================== 匠人端订单运营（P7） ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeliver(OrderBatchDeliverDTO dto) {
        Long craftsmanId = currentCraftsmanId();

        // ① 整批先校验，把所有不合格的订单一次性收集齐。
        //    遇到第一个就抛异常的话，操作员得改一单、重试、再发现下一单有问题，
        //    20 笔里混 3 笔能来回折腾好几轮。
        List<String> problems = new ArrayList<>();
        Map<Long, Order> orders = new LinkedHashMap<>();
        Map<Long, OrderBatchDeliverDTO.Item> itemMap = new LinkedHashMap<>();
        for (OrderBatchDeliverDTO.Item item : dto.getItems()) {
            Order order = orderMapper.selectById(item.getOrderId());
            if (order == null) {
                problems.add("订单 " + item.getOrderId() + " 不存在");
                continue;
            }
            if (!craftsmanId.equals(order.getCraftsmanId())) {
                problems.add(order.getOrderNo() + " 不属于你");
                continue;
            }
            if (!Integer.valueOf(Constants.ORDER_WAIT_DELIVER).equals(order.getStatus())) {
                problems.add(order.getOrderNo() + " 当前为「"
                        + OrderStatusEnum.of(order.getStatus()).getDesc() + "」，不可发货");
                continue;
            }
            // 同一个订单在入参里出现两次时按一次算，避免重复发货
            orders.put(order.getId(), order);
            itemMap.put(order.getId(), item);
        }
        BusinessException.throwIf(!problems.isEmpty(), ErrorCode.ORDER_STATUS_ERROR,
                "以下订单无法发货：" + String.join("；", problems));

        // ② 整批执行。任何一单在状态机校验上出问题都会抛异常，整批回滚——
        //    发货有物理后果，「有几单悄悄没发出去」比「整批重来一次」危险得多。
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, Order> entry : orders.entrySet()) {
            Order order = entry.getValue();
            OrderBatchDeliverDTO.Item item = itemMap.get(order.getId());
            OrderStatusEnum.checkTransfer(order.getStatus(), OrderStatusEnum.WAIT_RECEIVE);

            Order update = new Order();
            update.setId(order.getId());
            update.setStatus(Constants.ORDER_WAIT_RECEIVE);
            update.setLogisticsCompany(dto.getLogisticsCompany());
            update.setLogisticsNo(item.getLogisticsNo());
            // 只在传了备注时才写：否则批量发货会把先前单独填过的备注一并抹掉
            if (StringUtils.hasText(item.getCraftsmanRemark())) {
                update.setCraftsmanRemark(item.getCraftsmanRemark());
            }
            update.setDeliveryTime(now);
            orderMapper.updateById(update);
        }

        log.info("[订单] 匠人 {} 批量发货 {} 笔，物流={}", craftsmanId, orders.size(), dto.getLogisticsCompany());
        return orders.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remark(OrderRemarkDTO dto) {
        Order order = requireCraftsmanOrder(dto.getOrderId());
        String remark = dto.getCraftsmanRemark() == null ? null : dto.getCraftsmanRemark().trim();

        if (StringUtils.hasText(remark)) {
            Order update = new Order();
            update.setId(order.getId());
            update.setCraftsmanRemark(remark);
            orderMapper.updateById(update);
        } else {
            // 传空串 = 清空备注。updateById 会跳过 null 字段，清空这件事只能用显式 set 表达，
            // 否则「清空备注」会变成一个静默失败的操作
            orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, order.getId())
                    .set(Order::getCraftsmanRemark, null));
        }
        log.info("[订单] 匠人修改订单 {} 的备注", order.getOrderNo());
    }

    @Override
    public List<OrderExcelVO> exportOrders(OrderQueryDTO query) {
        query.normalize();
        Long craftsmanId = currentCraftsmanId();

        // 查询条件与列表接口逐条对齐，保证「页面上看到的」就是「导出的」；
        // 导出不分页但必须有上限，否则一次拉十几万条会把内存和数据库一起拖垮
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getCraftsmanId, craftsmanId)
                // 勾选导出：传了 ids 就只导这几单；craftsman_id 条件仍在，越权 id 查不出来
                .in(query.getIds() != null && !query.getIds().isEmpty(), Order::getId, query.getIds())
                .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getOrderNo()), Order::getOrderNo, query.getOrderNo())
                .eq(Boolean.TRUE.equals(query.getRefundOnly()), Order::getRefundStatus, 1)
                .orderByDesc(Order::getId)
                .last("LIMIT " + EXPORT_LIMIT);

        List<Order> orders = orderMapper.selectList(wrapper);
        List<OrderExcelVO> rows = assembleList(orders, true).stream()
                .map(this::toExcelRow)
                .toList();
        log.info("[订单] 匠人 {} 导出订单 {} 条", craftsmanId, rows.size());
        return rows;
    }

    /** 订单视图 → 导出行 */
    private OrderExcelVO toExcelRow(OrderVO vo) {
        OrderExcelVO row = new OrderExcelVO();
        row.setOrderNo(vo.getOrderNo());
        row.setCreateTime(fmtTime(vo.getCreateTime()));
        row.setBuyerName(vo.getBuyerName());
        row.setReceiver(vo.getReceiver());
        row.setReceiverPhone(vo.getReceiverPhone());
        row.setAddressSnapshot(vo.getAddressSnapshot());
        row.setItemSummary(itemSummary(vo.getItems()));
        row.setTotalQuantity(vo.getTotalQuantity());
        row.setTotalAmount(vo.getTotalAmount());
        row.setDiscountAmount(vo.getDiscountAmount());
        row.setPayAmount(vo.getPayAmount());
        row.setStatusDesc(vo.getStatusDesc());
        row.setLogisticsCompany(vo.getLogisticsCompany());
        row.setLogisticsNo(vo.getLogisticsNo());
        row.setDeliveryTime(fmtTime(vo.getDeliveryTime()));
        row.setRemark(vo.getRemark());
        row.setCraftsmanRemark(vo.getCraftsmanRemark());
        return row;
    }

    /**
     * 商品明细拼成一行文本
     *
     * <p>导出是给人看的表格，一个订单占一行；把订单项摊成多行会破坏「一行一单」的对账结构，
     * 所以这里拼成「商品名(规格)×2；商品名×1」。</p>
     */
    private static String itemSummary(List<OrderItemVO> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(i -> i.getGoodsName()
                        + (StringUtils.hasText(i.getSpecName()) ? "(" + i.getSpecName() + ")" : "")
                        + "×" + nvl(i.getQuantity()))
                .collect(Collectors.joining("；"));
    }

    private static String fmtTime(LocalDateTime time) {
        return time == null ? "" : EXPORT_TIME_FMT.format(time);
    }

    /* ================================================================== */

    private Order requireCraftsmanOrder(Long orderId) {
        Long craftsmanId = currentCraftsmanId();
        Order order = orderMapper.selectById(orderId);
        BusinessException.throwIf(order == null, ErrorCode.ORDER_NOT_FOUND);
        BusinessException.throwIf(!craftsmanId.equals(order.getCraftsmanId()),
                ErrorCode.ORDER_NOT_BELONG_TO_YOU);
        return order;
    }

    /** 取当前登录用户名下的匠人 id */
    /**
     * 取当前登录用户名下的匠人 id
     *
     * <p>P8 收口：原先这里与 {@link CraftsmanHelper} 各有一份实现，
     * 匠人资质审核状态只补在其中一份里 → 等于没校验。现统一走 CraftsmanHelper。</p>
     */
    private Long currentCraftsmanId() {
        return craftsmanHelper.currentCraftsmanId();
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private static String nvlStr(String v) {
        return v == null ? "" : v;
    }

    /**
     * 待下单项（下单过程中的中间载体）
     */
    @Data
    private static class PendingItem {
        private Long goodsId;
        private Long specId;
        private Long craftsmanId;
        private String goodsName;
        private String cover;
        private String specName;
        private BigDecimal price;
        private Integer stock;
        private Integer quantity;
    }
}
