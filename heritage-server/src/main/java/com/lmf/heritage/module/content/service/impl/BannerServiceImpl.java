package com.lmf.heritage.module.content.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.content.entity.Banner;
import com.lmf.heritage.module.content.mapper.BannerMapper;
import com.lmf.heritage.module.content.service.BannerService;
import com.lmf.heritage.module.content.vo.BannerVO;
import com.lmf.heritage.util.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 轮播图服务实现
 *
 * <p>缓存 key 按位置隔离（{@code banner:pos:1}、{@code banner:pos:2}），
 * 改首页轮播不会把课程页的缓存一起冲掉。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    /** 位置：1首页 */
    private static final int POSITION_HOME = 1;
    /** 前台单页最多返回的轮播条数（防止运营传了 50 张图导致首屏卡顿） */
    private static final int MAX_LIMIT = 10;

    private final BannerMapper bannerMapper;
    private final CacheHelper cache;

    @Override
    public List<BannerVO> listByPosition(Integer position, int limit) {
        int pos = position == null || position < 1 ? POSITION_HOME : position;
        int effective = limit <= 0 ? MAX_LIMIT : Math.min(limit, MAX_LIMIT);

        return cache.getListOrLoad(cacheKey(pos, effective), BannerVO.class, Constants.TTL_BANNER,
                () -> {
                    List<Banner> list = bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                            .eq(Banner::getPosition, pos)
                            .eq(Banner::getStatus, Constants.STATUS_ENABLED)
                            .orderByAsc(Banner::getSort)
                            .orderByAsc(Banner::getId)
                            .last("LIMIT " + effective));
                    log.debug("[轮播] 回源 position={} 共 {} 条", pos, list.size());
                    return list.stream().map(this::toVO).toList();
                });
    }

    @Override
    public void evictCache() {
        // 位置与条数的组合有限，按前缀整体清一次最省心
        cache.deleteByPrefix(Constants.KEY_BANNER_POS);
    }

    private String cacheKey(int position, int limit) {
        return Constants.KEY_BANNER_POS + position + ":" + limit;
    }

    private BannerVO toVO(Banner b) {
        BannerVO vo = new BannerVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setImage(b.getImage());
        vo.setLinkUrl(b.getLinkUrl());
        return vo;
    }
}
