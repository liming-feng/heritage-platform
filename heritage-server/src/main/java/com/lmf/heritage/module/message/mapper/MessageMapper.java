package com.lmf.heritage.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lmf.heritage.module.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 站内消息 Mapper
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /** 未读消息数（个人中心红点） */
    @Select("SELECT COUNT(*) FROM message WHERE deleted = 0 AND user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);

    /** 全部标记已读 */
    @Update("UPDATE message SET is_read = 1, update_time = NOW() "
            + "WHERE deleted = 0 AND user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);

    /**
     * 某张业务单据是否已经提醒过（定时任务幂等用）
     *
     * <p>开课提醒每次运行都可能重复扫到同一批预约，靠「同一预约同一类型只发一条」
     * 来判断是否已提醒，避免用户收到一串重复消息。</p>
     */
    @Select("SELECT COUNT(*) FROM message WHERE deleted = 0 AND user_id = #{userId} "
            + "AND type = #{type} AND related_id = #{relatedId}")
    int countByRelated(@Param("userId") Long userId,
                       @Param("type") Integer type,
                       @Param("relatedId") Long relatedId);

    /**
     * 同一张单据的<b>某一类</b>消息是否已发过（带标题的幂等判断）
     *
     * <p>为什么需要它：优惠券这一个消息类型下有两种通知 ——「券到账」与「券即将过期」，
     * 两者的 related_id 都指向同一张用户券。若只按 (user, type, relatedId) 去重，
     * 到账通知会把过期提醒「挡掉」。加上标题一起判断，两种通知各自独立去重。</p>
     */
    @Select("SELECT COUNT(*) FROM message WHERE deleted = 0 AND user_id = #{userId} "
            + "AND type = #{type} AND related_id = #{relatedId} AND title = #{title}")
    int countByRelatedAndTitle(@Param("userId") Long userId,
                              @Param("type") Integer type,
                              @Param("relatedId") Long relatedId,
                              @Param("title") String title);

    /**
     * 按消息类型统计总数与未读数（消息中心分类 tab 的角标）
     *
     * <p>一条 SQL 同时算出 total 与 unread：前者用于「该分类下共几条」，
     * 后者用于小红点。分两次查会多一趟往返，而且两次之间的数据可能已经变了。</p>
     */
    @Select("SELECT type, COUNT(*) AS total, "
            + "SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS unread "
            + "FROM message WHERE deleted = 0 AND user_id = #{userId} GROUP BY type")
    List<Map<String, Object>> countGroupByType(@Param("userId") Long userId);
}
