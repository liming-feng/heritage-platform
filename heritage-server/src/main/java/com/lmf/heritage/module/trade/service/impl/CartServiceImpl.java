package com.lmf.heritage.module.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.goods.entity.Goods;
import com.lmf.heritage.module.goods.entity.GoodsSpec;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.goods.mapper.GoodsSpecMapper;
import com.lmf.heritage.module.trade.dto.CartAddDTO;
import com.lmf.heritage.module.trade.dto.CartCheckDTO;
import com.lmf.heritage.module.trade.dto.CartUpdateDTO;
import com.lmf.heritage.module.trade.entity.Cart;
import com.lmf.heritage.module.trade.mapper.CartMapper;
import com.lmf.heritage.module.trade.service.CartService;
import com.lmf.heritage.module.trade.vo.CartItemVO;
import com.lmf.heritage.module.trade.vo.CartVO;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 购物车服务实现
 *
 * <p>购物车**不存商品快照**：名称、封面、价格、库存每次都按 goods / goods_spec 实时读取，
 * 这样商品改价后购物车立刻同步。快照只在下单那一刻写入 order_item。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final CartMapper cartMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsSpecMapper goodsSpecMapper;

    @Override
    public CartVO getCartDetail() {
        Long userId = UserContext.userId();
        List<CartItemVO> items = cartMapper.selectCartList(userId);
        items.forEach(CartItemVO::calcSubtotal);

        CartVO vo = new CartVO();
        vo.setItems(items);
        vo.setTotalCount(items.size());

        // 汇总只统计「已勾选 且 有效」的项，失效项不参与计算
        List<CartItemVO> validChecked = items.stream()
                .filter(i -> Constants.STATUS_ENABLED == nvl(i.getChecked()))
                .filter(i -> nvl(i.getInvalid()) == 0)
                .toList();

        vo.setCheckedCount(validChecked.size());
        vo.setCheckedQuantity(validChecked.stream()
                .mapToInt(i -> nvl(i.getQuantity()))
                .sum());
        vo.setCheckedAmount(validChecked.stream()
                .map(i -> i.getSubtotal() == null ? BigDecimal.ZERO : i.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP));
        vo.setAllValid(validChecked.size() == items.stream()
                .filter(i -> Constants.STATUS_ENABLED == nvl(i.getChecked()))
                .count());
        return vo;
    }

    @Override
    public void add(CartAddDTO dto) {
        Long userId = UserContext.userId();

        Goods goods = goodsMapper.selectById(dto.getGoodsId());
        BusinessException.throwIf(goods == null, ErrorCode.GOODS_NOT_FOUND);
        BusinessException.throwIf(nvl(goods.getStatus()) != 1 || nvl(goods.getAuditStatus()) != 1,
                ErrorCode.GOODS_OFF_SHELF);

        // 规格校验：规格必须存在且属于该商品
        GoodsSpec spec = null;
        if (dto.getSpecId() != null) {
            spec = goodsSpecMapper.selectById(dto.getSpecId());
            BusinessException.throwIf(spec == null || !goods.getId().equals(spec.getGoodsId()),
                    ErrorCode.PARAM_ERROR, "所选规格不存在");
        }

        // 加购时就校验库存，避免结算时才发现买不到
        int stock = spec != null ? nvl(spec.getStock()) : nvl(goods.getStock());
        BusinessException.throwIf(stock < dto.getQuantity(), ErrorCode.STOCK_NOT_ENOUGH,
                "库存不足，当前仅剩 " + stock + " 件");

        Cart exist = cartMapper.selectIncludingDeleted(userId, dto.getGoodsId(), dto.getSpecId());
        if (exist == null) {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setGoodsId(dto.getGoodsId());
            cart.setSpecId(dto.getSpecId());
            cart.setQuantity(dto.getQuantity());
            cart.setChecked(Constants.STATUS_ENABLED);
            cartMapper.insert(cart);
            log.debug("[购物车] 新增 goodsId={}, specId={}, qty={}", dto.getGoodsId(), dto.getSpecId(), dto.getQuantity());
        } else if (nvl(exist.getDeleted()) == 1) {
            // 之前被删过，直接复活（唯一键还在，不能重新 insert）
            cartMapper.revive(exist.getId(), dto.getQuantity());
            log.debug("[购物车] 复活已删除项 id={}, qty={}", exist.getId(), dto.getQuantity());
        } else {
            int newQuantity = nvl(exist.getQuantity()) + dto.getQuantity();
            BusinessException.throwIf(newQuantity > stock, ErrorCode.STOCK_NOT_ENOUGH,
                    "库存不足，购物车已有 " + exist.getQuantity() + " 件，当前仅剩 " + stock + " 件");
            Cart update = new Cart();
            update.setId(exist.getId());
            update.setQuantity(newQuantity);
            update.setChecked(Constants.STATUS_ENABLED);
            cartMapper.updateById(update);
            log.debug("[购物车] 累加数量 id={}, {} -> {}", exist.getId(), exist.getQuantity(), newQuantity);
        }
    }

    @Override
    public void updateQuantity(CartUpdateDTO dto) {
        Long userId = UserContext.userId();
        Cart cart = requireOwned(dto.getId(), userId);

        // 用实时库存校验（购物车页面停留期间库存可能已被别人买走）
        List<CartItemVO> items = cartMapper.selectByIdsAndUser(userId, List.of(cart.getId()));
        if (!items.isEmpty()) {
            CartItemVO item = items.get(0);
            int stock = nvl(item.getStock());
            BusinessException.throwIf(stock < dto.getQuantity(), ErrorCode.STOCK_NOT_ENOUGH,
                    "库存不足，当前仅剩 " + stock + " 件");
        }

        Cart update = new Cart();
        update.setId(cart.getId());
        update.setQuantity(dto.getQuantity());
        cartMapper.updateById(update);
    }

    @Override
    public void check(CartCheckDTO dto) {
        Long userId = UserContext.userId();
        int checked = nvl(dto.getChecked()) == 1 ? 1 : 0;
        cartMapper.updateChecked(userId, dto.getIds(), checked);
    }

    @Override
    public void remove(Long id) {
        Long userId = UserContext.userId();
        requireOwned(id, userId);
        cartMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBatch(List<Long> ids) {
        Long userId = UserContext.userId();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 一次查出归属，避免逐条 select（同时防越权删别人的购物车）
        List<CartItemVO> owned = cartMapper.selectByIdsAndUser(userId, ids);
        BusinessException.throwIf(owned.isEmpty(), ErrorCode.CART_ITEM_NOT_FOUND);
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, owned.stream().map(CartItemVO::getId).toList()));
    }

    @Override
    public void clear() {
        Long userId = UserContext.userId();
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    @Override
    public long count() {
        Long userId = UserContext.userId();
        return cartMapper.selectCount(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    @Override
    public List<CartItemVO> listCheckedItems() {
        return cartMapper.selectCartList(UserContext.userId()).stream()
                .peek(CartItemVO::calcSubtotal)
                .filter(i -> Constants.STATUS_ENABLED == nvl(i.getChecked()))
                .filter(i -> nvl(i.getInvalid()) == 0)
                .toList();
    }

    @Override
    public List<CartItemVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return cartMapper.selectByIdsAndUser(UserContext.userId(), ids).stream()
                .peek(CartItemVO::calcSubtotal)
                .toList();
    }

    @Override
    public void removeChecked() {
        cartMapper.deleteChecked(UserContext.userId());
    }

    /* ==================== 内部方法 ==================== */

    /** 取购物车项并校验归属，取不到或不属于本人时抛「购物车中没有该商品」 */
    private Cart requireOwned(Long id, Long userId) {
        Cart cart = cartMapper.selectById(id);
        BusinessException.throwIf(cart == null || !userId.equals(cart.getUserId()),
                ErrorCode.CART_ITEM_NOT_FOUND);
        return cart;
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
