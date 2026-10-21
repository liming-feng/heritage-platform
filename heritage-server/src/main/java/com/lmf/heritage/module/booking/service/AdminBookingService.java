package com.lmf.heritage.module.booking.service;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.booking.dto.AdminBookingQueryDTO;
import com.lmf.heritage.module.booking.vo.AdminBookingDetailVO;
import com.lmf.heritage.module.booking.vo.AdminBookingExcelVO;
import com.lmf.heritage.module.booking.vo.AdminBookingStatVO;
import com.lmf.heritage.module.booking.vo.BookingStatusLogVO;
import com.lmf.heritage.module.booking.vo.BookingVO;

import java.util.List;

/**
 * 管理端 · 预约监管与统计（P8）
 *
 * <p>与 {@code BookingService} 分开：那个接口承载的是「预约主线」——
 * 提交、审核、核销、取消这些会改数据、要保证并发正确性的动作；
 * 这里全是<b>只读</b>的监管视角（列表、详情、轨迹、5 项统计）。
 * 把只读的查询塞进主线接口，会让那个类越发难以审阅，而它恰恰是最需要看清楚的。</p>
 *
 * @author 李明峰 23100025
 */
public interface AdminBookingService {

    /** 全平台预约列表 */
    PageResult<BookingVO> page(AdminBookingQueryDTO query);

    /** 预约详情（含完整状态流转轨迹） */
    AdminBookingDetailVO detail(Long bookingId);

    /** 某条预约的流转轨迹 */
    List<BookingStatusLogVO> statusLogs(Long bookingId);

    /** 导出用：与列表同一套筛选条件 */
    List<AdminBookingExcelVO> exportList(AdminBookingQueryDTO query);

    /* ---------------- 5 项预约统计 ---------------- */

    /** ① 时段分布（按课程开课时段） */
    AdminBookingStatVO.SlotDistribution slotDistribution();

    /** ② 爽约率（只在已结束课程上算） */
    AdminBookingStatVO.AbsentRate absentRate(Integer topN);

    /** ③ 审核转化漏斗：提交 → 通过 → 到场 */
    AdminBookingStatVO.AuditFunnel auditFunnel();

    /** ④ 课程热度榜 */
    AdminBookingStatVO.CourseHeat courseHeat(Integer topN);

    /** ⑤ 近 N 天预约趋势（日期连续补零） */
    AdminBookingStatVO.Trend trend(Integer days);
}
