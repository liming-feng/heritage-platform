package com.lmf.heritage.module.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.module.trade.dto.CartAddDTO;
import com.lmf.heritage.module.trade.dto.CartCheckDTO;
import com.lmf.heritage.module.trade.dto.CartUpdateDTO;
import com.lmf.heritage.module.trade.entity.Cart;
import com.lmf.heritage.module.trade.vo.CartItemVO;
import com.lmf.heritage.module.trade.vo.CartVO;

import java.util.List;

/**
 * 购物车服务
 *
 * @author 李明峰 23100025
 */
public interface CartService extends IService<Cart> {

    /**
     * 购物车列表（含商品实时价格库存与失效标记）+ 勾选汇总
     *
     * <p>方法名不能叫 {@code list()}：{@code IService} 已经有一个
     * {@code List<T> list()}，返回类型不同会冲突（协变返回类型不适用）。</p>
     */
    CartVO getCartDetail();

    /** 加入购物车（同商品同规格累加数量） */
    void add(CartAddDTO dto);

    /** 修改数量 */
    void updateQuantity(CartUpdateDTO dto);

    /** 勾选 / 取消勾选（ids 为空表示全选或全不选） */
    void check(CartCheckDTO dto);

    /** 删除单项 */
    void remove(Long id);

    /** 批量删除 */
    void removeBatch(List<Long> ids);

    /** 清空购物车 */
    void clear();

    /** 购物车商品种类数（顶部角标用） */
    long count();

    /** 购物车中已勾选且有效的项（结算用） */
    List<CartItemVO> listCheckedItems();

    /** 按 id 集合取本人购物车项（校验归属，防越权结算别人的车） */
    List<CartItemVO> listByIds(List<Long> ids);

    /** 清空已勾选项（下单成功后调用） */
    void removeChecked();
}
