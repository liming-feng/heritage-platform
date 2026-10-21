package com.lmf.heritage.module.craftsman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.craftsman.entity.CraftsmanFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 匠人关注 Mapper
 *
 * <p>与购物车同款问题：表上有唯一键 {@code uk_user_craftsman(user_id, craftsman_id)}，
 * 而取消关注走逻辑删除。若再次关注时直接 INSERT 会撞唯一键，
 * 因此同样采用「查含已删除 → 复活 / 新建」的写法。</p>
 */
@Mapper
public interface CraftsmanFollowMapper extends BaseMapper<CraftsmanFollow> {

    /** 查关注关系（含已取消关注的行） */
    @Select("SELECT * FROM craftsman_follow WHERE user_id = #{userId} AND craftsman_id = #{craftsmanId} LIMIT 1")
    CraftsmanFollow selectIncludingDeleted(@Param("userId") Long userId,
                                          @Param("craftsmanId") Long craftsmanId);

    /** 复活一条已取消的关注关系（逻辑删除字段只能靠自定义 SQL 改动） */
    @Update("UPDATE craftsman_follow SET deleted = 0, update_time = NOW() WHERE id = #{id}")
    int revive(@Param("id") Long id);

    /** 该用户是否关注中 */
    @Select("SELECT COUNT(*) FROM craftsman_follow WHERE deleted = 0 AND user_id = #{userId} "
            + "AND craftsman_id = #{craftsmanId}")
    int countFollowing(@Param("userId") Long userId, @Param("craftsmanId") Long craftsmanId);

    /**
     * 我的关注列表（按关注时间倒序）
     *
     * <p>只返回仍通过资质审核的匠人——匠人被平台驳回后，他的卡片不该继续出现在用户列表里。</p>
     */
    @Select("SELECT cr.id, cr.real_name AS realName, cr.craft_type AS craftType, "
            + "cr.inheritance, cr.intro, cr.cover, cr.follower_count AS followerCount "
            + "FROM craftsman_follow f JOIN craftsman cr ON cr.id = f.craftsman_id "
            + "WHERE f.deleted = 0 AND f.user_id = #{userId} AND cr.deleted = 0 AND cr.audit_status = 1 "
            + "ORDER BY f.create_time DESC, f.id DESC")
    java.util.List<com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO> listFollowed(
            @Param("userId") Long userId);
}
