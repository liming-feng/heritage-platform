package com.lmf.heritage.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.content.dto.AdminBannerQueryDTO;
import com.lmf.heritage.module.content.entity.Banner;
import com.lmf.heritage.module.content.vo.AdminBannerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 轮播图 Mapper
 *
 * <p>P6 只有「按位置读启用图」这一条查询（用户端首页用），
 * P8 补上管理端的分页查询：要能按位置、状态筛选，且停用中的图也要看得见。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface BannerMapper extends BaseMapper<Banner> {

    /**
     * 管理端轮播图分页
     *
     * <p>与用户端不同，这里<b>不按 status 过滤</b>——停用的图如果从后台消失，
     * 管理员想重新启用就只能去改数据库。</p>
     */
    IPage<AdminBannerVO> selectAdminBannerPage(IPage<AdminBannerVO> page, @Param("q") AdminBannerQueryDTO q);

    /**
     * 该位置下「排在最后」的下一个排序号
     *
     * <p>新增轮播图不强制填排序号时用它兜底：不传就排到最后，
     * 而不是默认 0 —— 默认 0 会让新图直接顶掉首页第一屏的位置，
     * 运营还没配好图就已经上线了。</p>
     */
    @Select("SELECT COALESCE(MAX(sort), 0) + 1 FROM banner WHERE deleted = 0 AND position = #{position}")
    int nextSort(@Param("position") Integer position);
}
