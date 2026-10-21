package com.lmf.heritage.module.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.booking.dto.AdminBookingQueryDTO;
import com.lmf.heritage.module.booking.dto.BookingQueryDTO;
import com.lmf.heritage.module.booking.entity.Booking;
import com.lmf.heritage.module.booking.vo.BookingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预约 Mapper（题眼）
 *
 * <p>列表要同时带出课程标题 / 封面 / 匠人名 / 预约人昵称，走 XML 联表分页；
 * 状态流转相关的条件更新用注解 SQL 写清楚，让「原子性发生在哪一行」一目了然。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    /**
     * 预约分页查询
     *
     * @param userId       非空时只查该用户的预约（用户端）
     * @param craftsmanId  非空时只查该匠人课程的预约（匠人端）
     * @param timeoutHours 配合 {@code q.onlyTimeout} 使用，超过该小时数未审核视为超时
     */
    IPage<BookingVO> selectBookingPage(IPage<BookingVO> page,
                                       @Param("q") BookingQueryDTO q,
                                       @Param("userId") Long userId,
                                       @Param("craftsmanId") Long craftsmanId,
                                       @Param("timeoutHours") Integer timeoutHours);

    /** 按 id 查单条预约视图（字段与列表完全一致，避免两处维护） */
    BookingVO selectVoById(@Param("id") Long id);

    /**
     * <b>查同人同课的预约记录（含已被逻辑删除的行）</b>
     *
     * <p>三层防超卖的第三层是唯一键 {@code uk_user_course(user_id, course_id)}，
     * 它**对已取消 / 已拒绝的记录同样生效**。所以「取消后想重新预约」不能插新记录，
     * 必须把原来那条复活；这里把逻辑删除的行也查出来，供业务层判断。</p>
     */
    @Select("SELECT * FROM booking WHERE user_id = #{userId} AND course_id = #{courseId} LIMIT 1")
    Booking selectIncludingDeleted(@Param("userId") Long userId, @Param("courseId") Long courseId);

    /**
     * 复活一条已取消 / 已拒绝的预约：清空审核、取消、核销痕迹后重新提交。
     *
     * <p>状态由入参决定——需要审核时回到「待审核」，配置里关掉审核
     * （{@code audit_required=0}）时直接落到「已通过」并带上新生成的核销码。</p>
     *
     * <p>用自定义 SQL 而不是 {@code updateById}：逻辑删除字段被 MyBatis-Plus
     * 排除在普通 update 之外，改不动它；另外这里需要把若干字段显式置回 NULL，
     * 而 MP 的 {@code update-strategy: not_null} 会忽略 null 值。</p>
     */
    @Update("UPDATE booking SET deleted = 0, status = #{b.status}, quota = #{b.quota}, "
            + "contact_name = #{b.contactName}, contact_phone = #{b.contactPhone}, remark = #{b.remark}, "
            + "booking_no = #{b.bookingNo}, craftsman_id = #{b.craftsmanId}, "
            + "checkin_code = #{b.checkinCode}, checkin_time = NULL, "
            + "audit_time = #{b.auditTime}, auditor_id = #{b.auditorId}, "
            + "reject_reason = NULL, cancel_reason = NULL, cancel_by = NULL, cancel_time = NULL, "
            + "create_time = NOW(), update_time = NOW() "
            + "WHERE id = #{b.id}")
    int revive(@Param("b") Booking booking);

    /**
     * <b>乐观流转：只有当前状态等于 from 才更新为 to</b>
     *
     * <p>定时任务与并发审核靠这个条件保证幂等——两个线程同时处理同一条预约时，
     * 只有一个能更新成功（返回 1），另一个返回 0 后直接跳过副作用
     * （归还名额、发消息），不会重复执行。</p>
     */
    @Update("UPDATE booking SET status = #{to}, update_time = NOW() WHERE id = #{id} AND status = #{from}")
    int transferStatus(@Param("id") Long id, @Param("from") int from, @Param("to") int to);

    /* ------------------------------------------------------------------
     *  状态流转的「附带字段」更新
     *  都建立在 transferStatus 已经成功（即本线程抢到了这一次流转）的前提上，
     *  所以这里不必再带状态条件，写成简单 UPDATE 即可。
     * ------------------------------------------------------------------ */

    /** 审核通过：写入核销码、审核时间与审核人 */
    @Update("UPDATE booking SET checkin_code = #{checkinCode}, audit_time = #{auditTime}, "
            + "auditor_id = #{auditorId}, update_time = NOW() WHERE id = #{id}")
    int updateCheckin(@Param("id") Long id,
                      @Param("checkinCode") String checkinCode,
                      @Param("auditTime") LocalDateTime auditTime,
                      @Param("auditorId") Long auditorId);

    /** 审核拒绝 / 超时自动拒绝：写入拒绝原因与审核时间（系统操作时 auditorId 为 null） */
    @Update("UPDATE booking SET reject_reason = #{rejectReason}, audit_time = #{auditTime}, "
            + "auditor_id = #{auditorId}, update_time = NOW() WHERE id = #{id}")
    int updateAuditResult(@Param("id") Long id,
                          @Param("rejectReason") String rejectReason,
                          @Param("auditTime") LocalDateTime auditTime,
                          @Param("auditorId") Long auditorId);

    /** 取消：写入取消原因、取消发起方与取消时间 */
    @Update("UPDATE booking SET cancel_reason = #{cancelReason}, cancel_by = #{cancelBy}, "
            + "cancel_time = #{cancelTime}, update_time = NOW() WHERE id = #{id}")
    int updateCancelInfo(@Param("id") Long id,
                         @Param("cancelBy") Integer cancelBy,
                         @Param("cancelReason") String cancelReason,
                         @Param("cancelTime") LocalDateTime cancelTime);

    /** 核销：写入签到时间 */
    @Update("UPDATE booking SET checkin_time = #{checkinTime}, update_time = NOW() WHERE id = #{id}")
    int updateCheckinTime(@Param("id") Long id, @Param("checkinTime") LocalDateTime checkinTime);

    /** 用户端各状态预约数（tab 角标） */
    @Select("SELECT status, COUNT(*) AS cnt FROM booking WHERE deleted = 0 AND user_id = #{userId} GROUP BY status")
    List<Map<String, Object>> countGroupByStatusForUser(@Param("userId") Long userId);

    /** 匠人端各状态预约数 */
    @Select("SELECT status, COUNT(*) AS cnt FROM booking WHERE deleted = 0 AND craftsman_id = #{craftsmanId} "
            + "GROUP BY status")
    List<Map<String, Object>> countGroupByStatusForCraftsman(@Param("craftsmanId") Long craftsmanId);

    /** 某用户在某课程上「占着名额」的预约数（限约次数校验用） */
    @Select("SELECT COUNT(*) FROM booking WHERE deleted = 0 AND user_id = #{userId} AND course_id = #{courseId} "
            + "AND status IN (0, 1, 4, 5)")
    int countOccupied(@Param("userId") Long userId, @Param("courseId") Long courseId);

    /**
     * 扫描超时未审核的预约（定时任务用）
     *
     * @param deadline 早于该时间提交的待审核记录视为超时
     * @param limit    单批处理条数，避免一次拉太多GC压力大
     */
    @Select("SELECT * FROM booking WHERE deleted = 0 AND status = 0 AND create_time <= #{deadline} "
            + "ORDER BY id LIMIT #{limit}")
    List<Booking> selectTimeoutWaitAudit(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);

    /**
     * 查询「即将开课且已通过」的预约（开课提醒用）
     *
     * <p>已核销的不再提醒，所以条件里排除了状态 4。</p>
     */
    @Select("SELECT b.* FROM booking b JOIN course c ON c.id = b.course_id "
            + "WHERE b.deleted = 0 AND b.status = 1 AND c.deleted = 0 "
            + "AND c.start_time > NOW() AND c.start_time <= #{deadline}")
    List<Booking> selectUpcomingPassed(@Param("deadline") LocalDateTime deadline);

    /**
     * 课程结束后，把已核销的预约推进为「已完成」
     *
     * <p>批量更新，一条 SQL 处理一批，避免逐条 UPDATE。</p>
     */
    @Update("UPDATE booking SET status = 5, update_time = NOW() "
            + "WHERE deleted = 0 AND status = 4 AND course_id = #{courseId}")
    int finishCheckedInByCourse(@Param("courseId") Long courseId);

    /**
     * 某课程下「已核销待结课」的预约 id
     *
     * <p>配合 {@link #finishCheckedInByCourse} 使用：批量 UPDATE 只返回行数，
     * 而轨迹要逐条留痕，所以推进前先取一次 id。</p>
     */
    @Select("SELECT id FROM booking WHERE deleted = 0 AND status = 4 AND course_id = #{courseId} "
            + "ORDER BY id")
    List<Long> selectCheckedInIdsByCourse(@Param("courseId") Long courseId);

    /** 某课程的已核销预约数（用于「已核销 → 已完成」是否还有残留的判断） */
    @Select("SELECT COUNT(*) FROM booking WHERE deleted = 0 AND status = 4 AND course_id = #{courseId}")
    int countCheckedIn(@Param("courseId") Long courseId);

    /**
     * 某课程「还在生效中」的预约数（待审核 + 已通过）
     *
     * <p>删除课程前必须为 0：否则用户手里会留下一条指向不存在课程的预约。</p>
     */
    @Select("SELECT COUNT(*) FROM booking WHERE deleted = 0 AND course_id = #{courseId} AND status IN (0, 1)")
    int countActiveByCourse(@Param("courseId") Long courseId);

    /* ==================================================================
     *  管理端预约监管（P8）
     * ================================================================== */

    /**
     * 全平台预约分页
     *
     * <p>与用户端 / 匠人端共用同一份 {@code voColumns}：三端看到的一条预约
     * 必须是同一个东西。差别只在于过滤条件 —— 这里三个维度（用户、匠人、课程）
     * 都可以为 null，也可以同时限定。</p>
     */
    IPage<BookingVO> selectAdminBookingPage(IPage<BookingVO> page,
                                            @Param("q") AdminBookingQueryDTO q,
                                            @Param("timeoutHours") Integer timeoutHours);

    /**
     * 某条预约的审核人名称（管理端详情用）
     *
     * <p>刻意不把 {@code auditor_id} 加进共用的 {@code BookingVO}：
     * 那个 VO 是前台响应的一部分，多带一个 user id 对用户没有任何用处。
     * 这里单独查一次，只在管理端详情这一条链路上发生。</p>
     */
    @Select("SELECT IFNULL(NULLIF(u.nickname, ''), u.username) FROM booking b "
            + "LEFT JOIN `user` u ON u.id = b.auditor_id AND u.deleted = 0 "
            + "WHERE b.id = #{id}")
    String selectAuditorName(@Param("id") Long bookingId);

    /** ① 时段分布：按课程开课时间落在哪个时段聚合 */
    List<Map<String, Object>> statTimeSlot();    /**
     * ② 爽约率汇总
     *
     * <p>{@code WHERE c.status = 2} 把范围限定在已结束的课程上 ——
     * 课程没结束就谈不上「应到未到」。同时返回已结束课程数，
     * 供服务层判断「有没有数据」而决定爽约率给 null 还是百分比。</p>
     */
    List<Map<String, Object>> statAbsentOverall();

    /** ② 爽约率：单门课程（只含已结束课程，按爽约人次降序） */
    List<Map<String, Object>> statAbsentByCourse(@Param("topN") int topN);

    /** ③ 审核转化漏斗：各状态计数一次取完 */
    List<Map<String, Object>> statFunnel();

    /**
     * ④ 课程热度榜
     *
     * <p>排序主键与匠人看板一致：占用名额之和（status IN 0,1,4,5 的 quota 之和）。
     * 用 {@code LEFT JOIN} 保证「一个预约都没有的课程」也能出现在统计里（占用 0），
     * 否则运营会以为这门课不存在。</p>
     */
    List<Map<String, Object>> statCourseHeat(@Param("topN") int topN);

    /** ⑤ 近 N 天预约趋势（只返回有数据的日期，缺口由服务层补） */
    List<Map<String, Object>> statTrend(@Param("start") LocalDateTime start);
}
