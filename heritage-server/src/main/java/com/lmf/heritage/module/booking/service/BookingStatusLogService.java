package com.lmf.heritage.module.booking.service;

import com.lmf.heritage.module.booking.vo.BookingStatusLogVO;

import java.util.List;

/**
 * 预约状态流转轨迹服务
 *
 * <p>调用点分布在预约主线的每一个状态变化处（提交、审核、取消、核销、
 * 定时任务推进）。设计成「一行调用」而不是让各处自己拼实体，
 * 是为了让「状态变了就要留痕」这件事尽可能不容易被漏掉。</p>
 *
 * @author 李明峰 23100025
 */
public interface BookingStatusLogService {

    /**
     * 记录一次状态流转
     *
     * <p>操作方从 {@code UserContext} 取：已登录的用户 / 匠人 / 管理员按角色落库，
     * 未登录（定时任务）落成「系统」。这样定时任务和人工操作在轨迹里天然可区分。</p>
     *
     * @param bookingId  预约 id
     * @param fromStatus 原状态，{@code null} 表示创建
     * @param toStatus   新状态
     * @param remark     说明，可空
     */
    void record(Long bookingId, Integer fromStatus, int toStatus, String remark);

    /** 某条预约的完整流转轨迹（按发生顺序） */
    List<BookingStatusLogVO> listByBooking(Long bookingId);
}
