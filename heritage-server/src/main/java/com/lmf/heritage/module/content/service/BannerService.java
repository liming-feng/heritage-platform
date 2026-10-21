package com.lmf.heritage.module.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.module.content.entity.Banner;
import com.lmf.heritage.module.content.vo.BannerVO;

import java.util.List;

/**
 * 轮播图服务
 *
 * <p>前台只认「启用 + 按 sort 升序」的图；停用的图在这里被挡掉，
 * 而不是靠前端过滤 —— 前端过滤等于把下线的运营素材仍然发给浏览器。</p>
 *
 * @author 李明峰 23100025
 */
public interface BannerService extends IService<Banner> {

    /**
     * 按位置读取启用的轮播图
     *
     * @param position 位置：1首页 2课程页
     * @param limit    最多返回条数，小于等于 0 表示不限制
     */
    List<BannerVO> listByPosition(Integer position, int limit);

    /** 清除轮播缓存（新增/修改/停用轮播后调用） */
    void evictCache();
}
