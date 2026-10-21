package com.lmf.heritage.module.trade.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.trade.dto.AddressDTO;
import com.lmf.heritage.module.trade.entity.Address;
import com.lmf.heritage.module.trade.service.AddressService;
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
 * 用户端 · 收货地址
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-收货地址", description = "地址增删改查 / 默认地址")
@RestController
@RequestMapping("/portal/address")
@RequiredArgsConstructor
@LoginRequired
public class PortalAddressController {

    private final AddressService addressService;

    @Operation(summary = "我的地址列表", description = "默认地址排在最前")
    @GetMapping("/list")
    public Result<List<Address>> list() {
        return Result.success(addressService.listMine());
    }

    @Operation(summary = "地址详情")
    @GetMapping("/{id}")
    public Result<Address> detail(@PathVariable("id") Long id) {
        return Result.success(addressService.detail(id));
    }

    @Operation(summary = "默认地址", description = "没有默认地址时返回最近添加的一条；一条都没有返回 null")
    @GetMapping("/default")
    public Result<Address> defaultAddress() {
        return Result.success(addressService.getDefault());
    }

    @Operation(summary = "新增 / 修改地址", description = "带 id 为修改；第一条地址会自动成为默认地址")
    @PostMapping("/save")
    public Result<Long> save(@RequestBody @Valid AddressDTO dto) {
        return Result.success(addressService.save(dto));
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/default/{id}")
    public Result<Void> setDefault(@PathVariable("id") Long id) {
        addressService.setDefault(id);
        return Result.success();
    }

    @Operation(summary = "删除地址", description = "删除默认地址后会自动把最新的一条设为默认")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        addressService.remove(id);
        return Result.success();
    }
}
