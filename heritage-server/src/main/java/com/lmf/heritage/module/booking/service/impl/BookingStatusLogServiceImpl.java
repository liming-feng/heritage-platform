package com.lmf.heritage.module.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lmf.heritage.module.booking.entity.BookingStatusLog;
import com.lmf.heritage.module.booking.enums.BookingStatusEnum;
import com.lmf.heritage.module.booking.mapper.BookingStatusLogMapper;
import com.lmf.heritage.module.booking.service.BookingStatusLogService;
import com.lmf.heritage.module.booking.vo.BookingStatusLogVO;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 预约状态流转轨迹服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingStatusLogServiceImpl implements BookingStatusLogService {

    /** 操作方 4 = 系统（与 Constants.CANCEL_BY_SYSTEM 同一套编码） */
    private static final int OPERATOR_SYSTEM = 4;

    private final BookingStatusLogMapper bookingStatusLogMapper;

    @Override
    public void record(Long bookingId, Integer fromStatus, int toStatus, String remark) {
        if (bookingId == null) {
            return;
        }
        try {
            BookingStatusLog entity = new BookingStatusLog();
            entity.setBookingId(bookingId);
            entity.setFromStatus(fromStatus);
            entity.setToStatus(toStatus);
            entity.setRemark(truncate(remark));

            UserContext.LoginUser user = UserContext.get();
            if (user == null || user.getUserId() == null) {
                // 定时任务里没有登录上下文，正是「系统自动操作」的特征
                entity.setOperatorRole(OPERATOR_SYSTEM);
                entity.setOperatorName("系统");
            } else {
                entity.setOperatorId(user.getUserId());
                entity.setOperatorRole(user.getRole());
                entity.setOperatorName(user.getUsername());
            }
            bookingStatusLogMapper.insert(entity);
        } catch (Exception e) {
            // 轨迹是旁路能力：写不进去也不能让「预约审核通过」这种主业务回滚。
            // 与站内消息的异常兜底同一个原则 —— 主流程的成败不由附属记录决定
            log.warn("[预约轨迹] 写入失败 bookingId={} {}→{}", bookingId, fromStatus, toStatus, e);
        }
    }

    @Override
    public List<BookingStatusLogVO> listByBooking(Long bookingId) {
        List<BookingStatusLog> list = bookingStatusLogMapper.selectList(
                new LambdaQueryWrapper<BookingStatusLog>()
                        .eq(BookingStatusLog::getBookingId, bookingId)
                        .orderByAsc(BookingStatusLog::getId));
        return list.stream().map(this::toVO).toList();
    }

    private BookingStatusLogVO toVO(BookingStatusLog e) {
        BookingStatusLogVO vo = new BookingStatusLogVO();
        vo.setId(e.getId());
        vo.setFromStatus(e.getFromStatus());
        // 创建记录没有原状态：留空而不是填个「—」，前端据此决定要不要画箭头
        vo.setFromStatusDesc(e.getFromStatus() == null ? null : statusText(e.getFromStatus()));
        vo.setToStatus(e.getToStatus());
        vo.setToStatusDesc(statusText(e.getToStatus()));
        vo.setOperatorRole(e.getOperatorRole());
        vo.setOperatorRoleDesc(roleText(e.getOperatorRole()));
        vo.setOperatorName(e.getOperatorName());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    private static String statusText(Integer s) {
        if (s == null) {
            return null;
        }
        try {
            return BookingStatusEnum.of(s).getDesc();
        } catch (Exception e) {
            return "未知";
        }
    }

    private static String roleText(Integer role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case 1 -> "用户";
            case 2 -> "匠人";
            case 3 -> "管理员";
            case 4 -> "系统";
            default -> "未知";
        };
    }

    /** 原因类文本限长：remark 是 VARCHAR(255)，超长会直接抛 SQL 异常把业务带崩 */
    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 255 ? s : s.substring(0, 252) + "...";
    }
}
