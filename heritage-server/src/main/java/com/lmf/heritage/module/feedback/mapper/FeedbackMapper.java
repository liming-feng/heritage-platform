package com.lmf.heritage.module.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.feedback.dto.FeedbackQueryDTO;
import com.lmf.heritage.module.feedback.entity.Feedback;
import com.lmf.heritage.module.feedback.vo.FeedbackVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 留言反馈 Mapper
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    /**
     * 反馈分页，用户端与管理端共用
     *
     * @param userId 非空时只查该用户提交的（用户端）；为 null 时查全部（管理端）
     */
    IPage<FeedbackVO> selectFeedbackPage(IPage<FeedbackVO> page,
                                        @Param("userId") Long userId,
                                        @Param("q") FeedbackQueryDTO q);

    /**
     * 按状态分组的数量（处理时效统计）
     *
     * <p>刻意<b>只返回有数据的状态</b>而不是四个固定值：状态是封闭枚举，
     * 缺哪个就等于 0，由服务层补。让 SQL 返回「0 行」再在 Java 里补零，
     * 比在 SQL 里硬写 UNION 一堆 0 更容易读。</p>
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM feedback WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> selectStatusStat();

    /**
     * 按类型分组：总数、已完成数、平均处理时长（小时）
     *
     * <p>平均时长只对「已流转到已完成且有 handle_time」的记录计算 ——
     * 把处理中的算进来，平均值会被那些还没结束的反馈一直往上拉。</p>
     */
    @Select("SELECT f.type AS type, COUNT(*) AS cnt, "
            + "SUM(CASE WHEN f.status = 2 THEN 1 ELSE 0 END) AS doneCount, "
            + "ROUND(AVG(CASE WHEN f.handle_time IS NOT NULL "
            + "     THEN TIMESTAMPDIFF(HOUR, f.create_time, f.handle_time) END), 1) AS avgHours "
            + "FROM feedback f WHERE f.deleted = 0 GROUP BY f.type ORDER BY f.type ASC")
    List<Map<String, Object>> selectTypeStat();

    /** 已完成反馈的处理时长总览：条数、平均 / 最短 / 最长、24 小时内完成的条数 */
    @Select("SELECT COUNT(*) AS handledCount, "
            + "ROUND(AVG(TIMESTAMPDIFF(HOUR, create_time, handle_time)), 1) AS avgHours, "
            + "MIN(TIMESTAMPDIFF(HOUR, create_time, handle_time)) AS minHours, "
            + "MAX(TIMESTAMPDIFF(HOUR, create_time, handle_time)) AS maxHours, "
            + "SUM(CASE WHEN TIMESTAMPDIFF(HOUR, create_time, handle_time) <= 24 THEN 1 ELSE 0 END) AS within24h "
            + "FROM feedback WHERE deleted = 0 AND status = 2 AND handle_time IS NOT NULL")
    Map<String, Object> selectDurationStat();

    /**
     * 近 N 天每日提交量与完成量
     *
     * <p>两段 UNION 后合并：提交量按 {@code create_time} 归属，完成量按 {@code handle_time} 归属。
     * 刻意分开归因 —— 按提交日统计完成量，会让「上周的积压这周清掉了」这件事
     * 永远显示在上周的柱子上，看不出本周的工作量。</p>
     *
     * <p>只返回有数据的日期，缺的日期由服务层补 0：没有日历表可 JOIN，
     * 与其在 SQL 里拼一串日期，不如在 Java 里按 LocalDate 循环，可读性更好。</p>
     *
     * <p><b>注意这里的比较运算符写的是 {@code >=} 而不是 {@code &gt;=}</b>：
     * {@code &gt;} 是 XML 实体转义，只有写在 mapper.xml 里才会被解析；
     * 写在 {@code @Select} 注解的 Java 字符串里，它会被原样发给 MySQL，
     * 报的错是「near ';= '2026-09-05...'」这种看不出所以然的语法错误。</p>
     */
    @Select("SELECT day, COALESCE(SUM(submitCount), 0) AS submitCount, "
            + "COALESCE(SUM(doneCount), 0) AS doneCount FROM ( "
            + "  SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS day, COUNT(*) AS submitCount, 0 AS doneCount "
            + "  FROM feedback WHERE deleted = 0 AND create_time >= #{from} "
            + "  GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') "
            + "  UNION ALL "
            + "  SELECT DATE_FORMAT(handle_time, '%Y-%m-%d') AS day, 0 AS submitCount, COUNT(*) AS doneCount "
            + "  FROM feedback WHERE deleted = 0 AND status = 2 AND handle_time IS NOT NULL "
            + "    AND handle_time >= #{from} "
            + "  GROUP BY DATE_FORMAT(handle_time, '%Y-%m-%d') "
            + ") t GROUP BY day ORDER BY day ASC")
    List<Map<String, Object>> selectDailyStat(@Param("from") LocalDateTime from);

    /**
     * 处理反馈（管理员）
     *
     * <p>{@code handle_time} 只在流转到「已完成」时写入：用 CASE 让「完成时间」这个语义
     * 不被「处理中」的中间状态污染 —— 否则一条反馈会有好几个「完成时间」。
     * 已经是完成态的记录再次提交完成时，保留原来的完成时间（第一次完成才算数）。</p>
     */
    @Update("UPDATE feedback SET status = #{status}, "
            + "reply = COALESCE(#{reply}, reply), "
            + "handle_time = CASE WHEN #{status} = 2 THEN COALESCE(handle_time, #{now}) ELSE handle_time END, "
            + "update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0")
    int handle(@Param("id") Long id,
               @Param("status") Integer status,
               @Param("reply") String reply,
               @Param("now") LocalDateTime now);
}
