package com.lmf.heritage.module.trade.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.trade.dto.CartAddDTO;
import com.lmf.heritage.module.trade.dto.CartCheckDTO;
import com.lmf.heritage.module.trade.dto.CartUpdateDTO;
import com.lmf.heritage.module.trade.service.CartService;
import com.lmf.heritage.module.trade.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 · 购物车
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-购物车", description = "加入购物车 / 修改数量 / 勾选 / 删除")
@RestController
@RequestMapping("/portal/cart")
@RequiredArgsConstructor
@LoginRequired
public class PortalCartController {

    private final CartService cartService;

    @Operation(summary = "购物车列表", description = "含商品实时价格、库存与失效标记，以及勾选汇总金额")
    @GetMapping("/list")
    public Result<CartVO> list() {
        return Result.success(cartService.getCartDetail());
    }

    @Operation(summary = "加入购物车", description = "同商品同规格自动累加数量，加购时即校验库存")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Valid CartAddDTO dto) {
        cartService.add(dto);
        return Result.success();
    }

    @Operation(summary = "修改数量")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody @Valid CartUpdateDTO dto) {
        cartService.updateQuantity(dto);
        return Result.success();
    }

    @Operation(summary = "勾选 / 取消勾选", description = "ids 不传表示全选或全不选")
    @PutMapping("/check")
    public Result<Void> check(@RequestBody @Valid CartCheckDTO dto) {
        cartService.check(dto);
        return Result.success();
    }

    @Operation(summary = "删除单项")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        cartService.remove(id);
        return Result.success();
    }

    @Operation(summary = "批量删除")
    @PostMapping("/batch-delete")
    public Result<Void> removeBatch(@RequestBody List<Long> ids) {
        cartService.removeBatch(ids);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        cartService.clear();
        return Result.success();
    }

    @Operation(summary = "购物车数量", description = "顶部角标用")
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.success(cartService.count());
    }
}
