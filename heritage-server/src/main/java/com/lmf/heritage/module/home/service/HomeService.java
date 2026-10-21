package com.lmf.heritage.module.home.service;

import com.lmf.heritage.module.content.vo.BannerVO;
import com.lmf.heritage.module.course.vo.CourseSimpleVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import com.lmf.heritage.module.goods.vo.CategoryVO;
import com.lmf.heritage.module.goods.vo.GoodsListVO;

import java.util.List;

/**
 * 首页聚合服务
 *
 * <p>首页要拼轮播、分类、热门商品、推荐匠人、体验活动预告 —— 数据来自 5 个不同模块。
 * 抽成独立模块而不是塞进 goods，是为了避免 goods ↔ craftsman ↔ course 互相 import 形成循环依赖。</p>
 *
 * @author 李明峰 23100025
 */
public interface HomeService {

    /** 首页轮播（缓存 30 分钟） */
    List<BannerVO> banners();

    /** 分类导航（缓存 1 小时，与 CategoryService 共用同一个 key） */
    List<CategoryVO> categories();

    /** 热门商品（缓存 10 分钟） */
    List<GoodsListVO> hotGoods();

    /** 推荐匠人（缓存 30 分钟） */
    List<CraftsmanSimpleVO> recommendCraftsmen();

    /** 体验活动预告：即将开课且尚可预约的课程（缓存 10 分钟） */
    List<CourseSimpleVO> upcomingCourses();

    /** 清首页缓存（后台改动轮播 / 匠人 / 课程后调用） */
    void evictCache();
}
