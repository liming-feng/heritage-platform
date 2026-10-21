package com.lmf.heritage.module.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.course.dto.AdminCourseQueryDTO;
import com.lmf.heritage.module.course.dto.CourseQueryDTO;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.vo.AdminCourseDetailVO;
import com.lmf.heritage.module.course.vo.AdminCourseVO;
import com.lmf.heritage.module.course.vo.CourseListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 课程 Mapper
 *
 * <p>列表要带出分类名与匠人名，走 XML 联表分页（见 {@code resources/mapper/course/CourseMapper.xml}）；
 * 名额相关的操作全部写成注解 SQL —— 这几条语句是「题眼」的核心，
 * 把「判断条件写在 UPDATE 的 WHERE 里」这件事摆在最显眼的位置，比藏在 XML 里更好审。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 把 Redis 累计的浏览量增量回写到 MySQL（定时任务用）
     *
     * <p>直接写注解 SQL：语句很短，单独开 XML 反而不易读。</p>
     */
    @Update("UPDATE course SET view_count = view_count + #{delta} WHERE id = #{id} AND deleted = 0")
    int addViewCount(@Param("id") Long id, @Param("delta") long delta);

    /* ==================================================================
     *  名额并发防护（题眼：防超卖第二层）
     * ================================================================== */

    /**
     * <b>数据库行锁读取课程（防超卖第二层 a）</b>
     *
     * <p>{@code FOR UPDATE} 会对这一行加排他锁并持有到事务提交。同一门课的并发预约请求
     * 会在这里排队，读到的一定是「前一个事务已经扣减完」的数量，
     * 因此后面接着做的 {@code remain_quota >= quota} 判断不会有竞态窗口。</p>
     *
     * <p>注意：必须与调用方处于同一个 {@code @Transactional} 事务中才有意义——
     * 事务一结束锁就释放了。</p>
     */
    @Select("SELECT * FROM course WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Course selectForUpdate(@Param("id") Long id);

    /**
     * <b>扣减名额（防超卖第二层的原子落点）</b>
     *
     * <p>{@code AND remain_quota >= #{quota}} 让「判断 + 扣减」变成一条 SQL 里的原子操作。
     * 返回 0 行即表示名额已被抢完，业务层抛异常回滚整单。</p>
     *
     * <p>同时累加 {@code booking_count}（累计预约数），保证「总名额 - 剩余名额」与它始终对得上。</p>
     */
    @Update("UPDATE course SET remain_quota = remain_quota - #{quota}, "
            + "booking_count = booking_count + #{quota}, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0 AND remain_quota >= #{quota}")
    int deductQuota(@Param("id") Long id, @Param("quota") int quota);

    /**
     * 归还名额（拒绝 / 用户取消 / 管理员取消 / 系统超时四条路径共用）
     *
     * <p>{@code remain_quota + quota} 用 {@code LEAST(total_quota, ...)} 兜住上限：
     * 一旦出现重复归还（并发或重试），剩余名额也不会超过总名额，
     * 从而不会凭空「多出名额」卖超。这是归还路径的自我防护。</p>
     */
    @Update("UPDATE course SET remain_quota = LEAST(total_quota, remain_quota + #{quota}), "
            + "booking_count = GREATEST(booking_count - #{quota}, 0), update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0")
    int restoreQuota(@Param("id") Long id, @Param("quota") int quota);

    /** 课程剩余名额（实时值，用于归还前的上限校验与日志） */
    @Select("SELECT remain_quota FROM course WHERE id = #{id} AND deleted = 0")
    Integer selectRemainQuota(@Param("id") Long id);

    /* ==================================================================
     *  课程状态推进（定时任务）
     * ================================================================== */

    /**
     * 按时间推进课程状态（0未开始 → 1进行中 → 2已结束）
     *
     * <p>带 {@code status <> #{status}} 条件是为了让重复执行变成空操作：
     * 定时任务每分钟都可能扫到同一门课，但状态已经推进过的行不会再次被更新，
     * 返回受影响行数也就是「本次真正推进了几门课」，日志里一眼能看出来。</p>
     *
     * @return 受影响行数
     */
    @Update("UPDATE course SET status = #{status}, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0 AND status <> #{status}")
    int pushStatus(@Param("id") Long id, @Param("status") int status);

    /** 已到开课时间但仍标记为「未开始」的课程 */
    @Select("SELECT * FROM course WHERE deleted = 0 AND status = 0 AND start_time <= NOW() "
            + "ORDER BY id LIMIT #{limit}")
    List<Course> selectStartDue(@Param("limit") int limit);

    /**
     * 已过结束时间但尚未标记为「已结束」的课程
     *
     * <p>条件写成 {@code status IN (0, 1)} 而不是 {@code status = 1}：
     * 定时任务若是中途才启动（或停机一段时间），会存在「开课时间与结束时间都已过去、
     * 状态却还停在未开始」的课程。只看 1 会漏掉它们，让课程永远卡在未开始、预约也永远完不成。</p>
     */
    @Select("SELECT * FROM course WHERE deleted = 0 AND status IN (0, 1) AND end_time <= NOW() "
            + "ORDER BY id LIMIT #{limit}")
    List<Course> selectEndDue(@Param("limit") int limit);

    /* ==================================================================
     *  查询与统计
     * ================================================================== */

    /** 前台课程列表分页（联表出分类名、匠人名，并算出倒计时所需的原始字段） */
    IPage<CourseListVO> selectCoursePage(IPage<CourseListVO> page, @Param("q") CourseQueryDTO query);

    /**
     * 按 id 查单条课程视图
     *
     * <p>与列表共用同一份 SQL 片段，避免「列表有某个字段、详情没有」这类不一致。</p>
     */
    CourseListVO selectCourseVoById(@Param("id") Long id);

    /** 匠人端「我的课程」分页（不受 audit_status 与课程状态限制） */
    IPage<CourseListVO> selectMyCoursePage(IPage<CourseListVO> page,
                                          @Param("craftsmanId") Long craftsmanId,
                                          @Param("q") CourseQueryDTO query);

    /**
     * 匠人端日历排期：取某日期区间内开课的课程（按开课时间升序）
     *
     * <p>区间用的是左闭右开 {@code [start, end)}：调用方传入「某日 00:00」到「次日 00:00」
     * 就能精确表达一天而不会把当天的 23:59 漏掉，也不会把次日 00:00 多算进来。</p>
     */
    List<CourseListVO> selectCalendarList(@Param("craftsmanId") Long craftsmanId,
                                          @Param("start") java.time.LocalDateTime start,
                                          @Param("end") java.time.LocalDateTime end);

    /** 某匠人开设的课程数 */
    @Select("SELECT COUNT(*) FROM course WHERE deleted = 0 AND audit_status = 1 AND craftsman_id = #{craftsmanId}")
    long countByCraftsman(@Param("craftsmanId") Long craftsmanId);

    /** 某匠人的课程列表（匠人主页展示，不分页，取最近若干门） */
    @Select("SELECT id, title, subtitle, cover, price, address, start_time AS startTime, end_time AS endTime, "
            + "booking_deadline AS bookingDeadline, cancel_deadline AS cancelDeadline, total_quota AS totalQuota, "
            + "remain_quota AS remainQuota, (total_quota - remain_quota) AS bookedQuota, duration, difficulty, "
            + "view_count AS viewCount, booking_count AS bookingCount, status, craftsman_id AS craftsmanId "
            + "FROM course WHERE deleted = 0 AND audit_status = 1 AND craftsman_id = #{craftsmanId} "
            + "ORDER BY status ASC, start_time ASC LIMIT #{limit}")
    List<CourseListVO> listByCraftsman(@Param("craftsmanId") Long craftsmanId, @Param("limit") int limit);

    /** 课程评价统计（评价在 review 表，type=2 表示课程评价） */
    @Select("SELECT COUNT(*) AS cnt, IFNULL(AVG(rating), 0) AS avgScore FROM review "
            + "WHERE deleted = 0 AND type = 2 AND target_id = #{courseId}")
    Map<String, Object> reviewStat(@Param("courseId") Long courseId);

    /** 各状态课程数（供首页/统计使用） */
    @Select("SELECT status, COUNT(*) AS cnt FROM course WHERE deleted = 0 AND audit_status = 1 GROUP BY status")
    List<Map<String, Object>> countGroupByStatus();

    /** 各分类下的课程数（不分状态），用于分类管理页的统计与删除前判断 */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM course "
            + "WHERE deleted = 0 GROUP BY category_id")
    List<Map<String, Object>> countGroupByCategory();

    /* ==================================================================
     *  管理端课程审核（P8）
     * ================================================================== */

    /** 管理端全平台课程分页（复用 voColumns/voJoins，追加审核相关字段） */
    IPage<AdminCourseVO> selectAdminCoursePage(IPage<AdminCourseVO> page,
                                              @Param("q") AdminCourseQueryDTO query);

    /**
     * 课程审核详情
     *
     * <p>报名结构用 6 个标量子查询在<b>同一行</b>上取完：这是一条单行查询，
     * 不存在 N+1 的问题，比拆成「先查课程、再查 6 个 COUNT」少 6 次网络往返。
     * 若哪天要按状态分组统计多门课，就该改回 GROUP BY 的写法。</p>
     */
    AdminCourseDetailVO selectAdminCourseDetail(@Param("id") Long id);

    /**
     * 课程审核状态流转（通过 / 驳回 / 违规下架共用）
     *
     * <p>只动 {@code audit_status} 与 {@code reject_reason}，<b>不碰 status</b> ——
     * 课程状态由收/开课时间这个事实决定（定时任务推进），不是审核结果能改的。
     * 这一点与商品不同：商品没有「事实状态」，只有上下架意图。</p>
     *
     * @param fromAuditStatus 期望的当前审核状态（并发护栏）
     * @return 实际受影响行数
     */
    int updateAuditByIds(@Param("ids") List<Long> ids,
                         @Param("fromAuditStatus") int fromAuditStatus,
                         @Param("auditStatus") int auditStatus,
                         @Param("rejectReason") String rejectReason);
}
