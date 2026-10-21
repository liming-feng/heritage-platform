package com.lmf.heritage.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.user.dto.AdminUserQueryDTO;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.vo.AdminUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper
 *
 * <p>启动类上已有 {@code @MapperScan("com.lmf.heritage.module.**.mapper")}，
 * 这里再标 @Mapper 也不冲突，方便单独看代码时知道它是 Mapper。</p>
 *
 * <p>管理端的三条查询放在这里，而不是各写一个 AdminUserMapper：
 * 它们查的都是 {@code user} 表（列表额外 LEFT JOIN craftsman），
 * 按「表」而不是按「页面」归置更省事，也不会出现两份对 user 表的理解。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 管理端用户分页（role=2 时顺带带出匠人资质字段）
     *
     * <p>LEFT JOIN 而不是 INNER JOIN：普通用户与管理员在 craftsman 表里没有对应行，
     * 用 INNER JOIN 会把「用户管理」页直接变成「匠人管理」页。</p>
     */
    IPage<AdminUserVO> selectAdminUserPage(IPage<AdminUserVO> page, @Param("q") AdminUserQueryDTO query);

    /**
     * 按天统计注册量（只返回有注册的日期，缺口由服务层补 0）
     *
     * @return [{statDay: '2026-09-18', cnt: 3}, ...]
     */
    List<Map<String, Object>> selectRegisterDayTrend(@Param("start") LocalDateTime start);

    /**
     * 按月统计注册量
     *
     * @return [{statMonth: '2026-09', cnt: 5}, ...]
     */
    List<Map<String, Object>> selectRegisterMonthTrend(@Param("start") LocalDateTime start);

    /** 角色分布 [{role: 1, cnt: 10}, ...] */
    List<Map<String, Object>> selectRoleCounts();
}
