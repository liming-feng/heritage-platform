package com.lmf.heritage.module.booking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.booking.dto.BookingAuditDTO;
import com.lmf.heritage.module.booking.dto.BookingCancelDTO;
import com.lmf.heritage.module.booking.dto.BookingCheckinDTO;
import com.lmf.heritage.module.booking.dto.BookingQueryDTO;
import com.lmf.heritage.module.booking.dto.BookingSubmitDTO;
import com.lmf.heritage.module.booking.entity.Booking;
import com.lmf.heritage.module.booking.vo.BookingVO;
import com.lmf.heritage.module.booking.vo.CheckinResultVO;

import java.util.List;
import java.util.Map;

/**
 * 课程预约服务（<b>课题题眼</b>）
 *
 * <p>围绕「提交 → 审核 → 核销 → 完成」这条主线，外加四条归还名额的支线
 * （审核拒绝 / 用户取消 / 管理员取消 / 系统超时自动拒绝）。</p>
 *
 * @author 李明峰 23100025
 */
public interface BookingService extends IService<Booking> {

    /* ==================== 用户端 ==================== */

    /**
     * 提交预约
     *
     * <p>三层防超卖全部在这一条链路上（Redis 锁 → 数据库行锁与条件更新 → 唯一索引），
     * 详见实现类里的注释。</p>
     *
     * @return 预约 id
     */
    Long submit(BookingSubmitDTO dto);

    /** 提交预约并返回完整详情（把核销码一并带回，前端提交后可直接展示） */
    BookingVO submitAndDetail(BookingSubmitDTO dto);

    /** 我的预约（支持状态筛选 / 关键词 / 分页） */
    PageResult<BookingVO> myBookings(BookingQueryDTO query);

    /** 我的各状态预约数（tab 角标） */
    Map<String, Long> myStatusCount();

    /** 预约详情（只能看自己的） */
    BookingVO detail(Long bookingId);

    /** 取核销码（仅「已通过 / 已核销」可看，前端用它渲染二维码） */
    String checkinCode(Long bookingId);

    /** 用户取消预约（归还名额） */
    void cancel(BookingCancelDTO dto);

    /* ==================== 匠人端 ==================== */

    /** 预约列表（只看自己课程的预约，支持状态 / 超时未审筛选） */
    PageResult<BookingVO> craftsmanBookings(BookingQueryDTO query);

    /** 匠人端各状态预约数 */
    Map<String, Long> craftsmanStatusCount();

    /** 审核预约：通过（生成核销码）或 拒绝（必填原因 + 归还名额） */
    void audit(BookingAuditDTO dto);

    /** 核销签到：凭用户出示的核销码把预约推进为「已核销」 */
    CheckinResultVO checkin(BookingCheckinDTO dto);

    /** 匠人取消预约（如场地变更），归还名额，cancel_by=2 */
    void craftsmanCancel(BookingCancelDTO dto);

    /** 某课程的报名名单（匠人端查看 / 导出用） */
    List<BookingVO> roster(Long courseId);

    /* ==================== 管理端 ==================== */

    /** 管理员强制取消预约，不校验用户侧的取消时限，cancel_by=3 */
    void adminCancel(BookingCancelDTO dto);

    /* ==================== 定时任务入口 ==================== */

    /** 超时未审核的预约自动拒绝 + 归还名额 + 站内通知，返回处理条数 */
    int autoRejectTimeout();

    /** 按时间推进课程状态，并把「已核销」的预约推进为「已完成」，返回推进的课程数 */
    int autoFinishCourses();

    /** 给 24 小时内开课且已通过的用户发开课提醒，返回新增提醒条数 */
    int sendCourseReminders();
}
