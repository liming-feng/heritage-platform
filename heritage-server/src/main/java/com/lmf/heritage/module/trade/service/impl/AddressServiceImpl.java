package com.lmf.heritage.module.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.trade.dto.AddressDTO;
import com.lmf.heritage.module.trade.entity.Address;
import com.lmf.heritage.module.trade.mapper.AddressMapper;
import com.lmf.heritage.module.trade.service.AddressService;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务实现
 *
 * <p>「默认地址唯一」这条约束靠业务层维护：任何一次设置默认，
 * 都先把该用户其余地址的 {@code is_default} 清零，再设目标地址。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<Address> listMine() {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, UserContext.userId())
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getUpdateTime)
                .orderByDesc(Address::getId));
    }

    @Override
    public Address detail(Long id) {
        return requireOwned(id, UserContext.userId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(AddressDTO dto) {
        Long userId = UserContext.userId();

        Address entity = new Address();
        entity.setId(dto.getId());
        entity.setUserId(userId);
        entity.setReceiver(dto.getReceiver());
        entity.setPhone(dto.getPhone());
        entity.setProvince(dto.getProvince());
        entity.setCity(dto.getCity());
        entity.setDistrict(dto.getDistrict());
        entity.setDetail(dto.getDetail());

        boolean isDefault = nvl(dto.getIsDefault()) == 1;
        boolean isNew = dto.getId() == null;

        if (isNew) {
            // 第一条地址自动成为默认地址（否则用户下单时会没有可选地址）
            long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId));
            boolean asDefault = count == 0 || isDefault;
            entity.setIsDefault(asDefault ? 1 : 0);
            addressMapper.insert(entity);
            // 新增时若指定为默认，同样要清掉其余地址的默认标记 —— 漏掉这一步会出现两个默认地址
            if (asDefault) {
                clearOtherDefault(userId, entity.getId());
            }
            log.debug("[地址] 新增 id={}, 默认={}", entity.getId(), entity.getIsDefault());
            return entity.getId();
        }

        requireOwned(dto.getId(), userId);
        entity.setIsDefault(isDefault ? 1 : 0);
        addressMapper.updateById(entity);
        if (isDefault) {
            clearOtherDefault(userId, dto.getId());
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Long userId = UserContext.userId();
        Address address = requireOwned(id, userId);
        addressMapper.deleteById(id);

        // 删掉的是默认地址 → 把剩下最近更新的一条顶上来，避免"有地址但没有默认地址"
        if (nvl(address.getIsDefault()) == 1) {
            List<Address> rest = listMine();
            if (!rest.isEmpty()) {
                Address first = rest.get(0);
                Address update = new Address();
                update.setId(first.getId());
                update.setIsDefault(1);
                addressMapper.updateById(update);
                log.debug("[地址] 默认地址已删除，自动切换为 id={}", first.getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Long userId = UserContext.userId();
        requireOwned(id, userId);
        Address update = new Address();
        update.setId(id);
        update.setIsDefault(1);
        addressMapper.updateById(update);
        clearOtherDefault(userId, id);
    }

    @Override
    public Address getDefault() {
        Long userId = UserContext.userId();
        Address address = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .orderByDesc(Address::getUpdateTime)
                .last("LIMIT 1"));
        if (address != null) {
            return address;
        }
        return addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getUpdateTime)
                .last("LIMIT 1"));
    }

    @Override
    public Address requireOwned(Long id, Long userId) {
        BusinessException.throwIf(id == null, ErrorCode.ADDRESS_NOT_FOUND);
        Address address = addressMapper.selectById(id);
        BusinessException.throwIf(address == null || !userId.equals(address.getUserId()),
                ErrorCode.ADDRESS_NOT_FOUND);
        return address;
    }

    /** 把该用户除 keepId 以外的地址默认标记清零 */
    private void clearOtherDefault(Long userId, Long keepId) {
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .ne(Address::getId, keepId)
                .set(Address::getIsDefault, 0)
                .set(Address::getUpdateTime, java.time.LocalDateTime.now()));
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
