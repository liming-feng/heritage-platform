package com.lmf.heritage.module.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.module.trade.dto.AddressDTO;
import com.lmf.heritage.module.trade.entity.Address;

import java.util.List;

/**
 * 收货地址服务
 *
 * @author 李明峰 23100025
 */
public interface AddressService extends IService<Address> {

    /** 我的地址列表（默认地址排最前） */
    List<Address> listMine();

    /** 地址详情（校验归属） */
    Address detail(Long id);

    /** 新增或修改，返回地址 id */
    Long save(AddressDTO dto);

    /** 删除地址（删掉默认地址时会自动把最新的一条设为默认） */
    void remove(Long id);

    /** 设为默认地址（同时把其余地址的默认标记清掉） */
    void setDefault(Long id);

    /** 取默认地址，没有默认则取最近添加的一条；一条都没有时返回 null */
    Address getDefault();

    /** 取地址并校验是否属于指定用户（下单时用） */
    Address requireOwned(Long id, Long userId);
}
