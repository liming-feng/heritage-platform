package com.lmf.heritage.module.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.favorite.entity.Favorite;
import com.lmf.heritage.module.favorite.vo.FavoriteVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 收藏 Mapper
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * <b>新增收藏（幂等 upsert）</b>
     *
     * <pre>
     * INSERT INTO favorite (user_id, target_type, target_id, deleted, create_time, update_time)
     * VALUES (#{userId}, #{targetType}, #{targetId}, 0, NOW(), NOW())
     * ON DUPLICATE KEY UPDATE deleted = 0, update_time = NOW()
     * </pre>
     *
     * <p>为什么要 upsert 而不是 insert：唯一键 {@code uk_user_target} 对<b>已逻辑删除</b>的行
     * 同样生效，所以「取消收藏后再点收藏」用 insert 必然撞键。
     * ON DUPLICATE KEY UPDATE 把老记录复活，一条 SQL 就同时处理了「首次收藏」与「再次收藏」。
     * 返回值：插入为 1、命中重复键更新为 2（MySQL 语义），两者都大于 0。</p>
     */
    @Insert("INSERT INTO favorite (user_id, target_type, target_id, deleted, create_time, update_time) "
            + "VALUES (#{userId}, #{targetType}, #{targetId}, 0, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE deleted = 0, update_time = NOW()")
    int upsert(@Param("userId") Long userId,
               @Param("targetType") Integer targetType,
               @Param("targetId") Long targetId);

    /**
     * 取消收藏（逻辑删除）
     *
     * <p>带 {@code deleted = 0} 条件，重复取消只会生效一次 ——
     * 调用方拿返回行数就能区分「真的取消了」与「本来就没收藏」。</p>
     */
    @Update("UPDATE favorite SET deleted = 1, update_time = NOW() "
            + "WHERE user_id = #{userId} AND target_type = #{targetType} "
            + "AND target_id = #{targetId} AND deleted = 0")
    int remove(@Param("userId") Long userId,
               @Param("targetType") Integer targetType,
               @Param("targetId") Long targetId);

    /** 是否已收藏 */
    @Select("SELECT COUNT(*) FROM favorite WHERE deleted = 0 "
            + "AND user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int countByUserAndTarget(@Param("userId") Long userId,
                             @Param("targetType") Integer targetType,
                             @Param("targetId") Long targetId);

    /** 我的收藏各类型数量（tab 角标） */
    @Select("SELECT target_type AS targetType, COUNT(*) AS cnt FROM favorite "
            + "WHERE deleted = 0 AND user_id = #{userId} GROUP BY target_type")
    List<Map<String, Object>> countGroupByType(@Param("userId") Long userId);

    /**
     * 我的收藏分页
     *
     * <p>商品 / 课程 / 资讯三张表用「带条件的三次 LEFT JOIN」一次带出来，而不是
     * 先查收藏再逐条回查目标表（N+1），也不是用 UNION 拼三条 SQL。
     * 每个 JOIN 都带上 {@code f.target_type = ?} 条件，所以一趟扫描只会命中其中一张表。</p>
     */
    IPage<FavoriteVO> selectMyPage(IPage<FavoriteVO> page,
                                  @Param("userId") Long userId,
                                  @Param("targetType") Integer targetType);
}
