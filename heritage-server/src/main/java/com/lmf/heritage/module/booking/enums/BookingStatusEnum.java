package com.lmf.heritage.module.booking.enums;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预约状态机（<b>题眼</b>）
 *
 * <pre>
 *                       ┌── 审核通过 ──→ 已通过(1) ── 开课核销 ──→ 已核销(4) ── 课程结束 ──→ 已完成(5)
 *   待审核(0) ─────────┤                  └── 用户/管理员取消 ──→ 已取消(3)
 *                       ├── 审核拒绝 ──→ 已拒绝(2)
 *                       ├── 用户取消 ──→ 已取消(3)
 *                       └── 超时未审 ──→ 已拒绝(2)（cancel_by=4 系统）
 * </pre>
 *
 * <p>设计要点：状态流转用一张 {@code Map<当前状态, 允许流转到的集合>} 表驱动，
 * 所有变更统一走 {@link #checkTransfer(Integer, BookingStatusEnum)}。
 * 业务代码里不出现 {@code if (status == 1 || status == 4)} 这类散落的判断，
 * 新增状态时只改这一处。</p>
 *
 * <p>关于「已通过(1) 但课程结束后仍未核销」：本系统**有意不把它流转为已完成**，
 * 让它保持「已通过」以留作**爽约**记录。已完成代表「真的到场参加了」，
 * 二者混为一谈会让 P8 的爽约率统计失去数据基础。</p>
 *
 * @author 李明峰 23100025
 */
@Getter
public enum BookingStatusEnum {

    WAIT_AUDIT(0, "待审核"),
    PASSED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    CANCELED(3, "已取消"),
    CHECKED_IN(4, "已核销"),
    FINISHED(5, "已完成");

    private final int code;
    private final String desc;

    BookingStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private static final Map<Integer, BookingStatusEnum> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(BookingStatusEnum::getCode, e -> e));

    /**
     * 状态流转表：key 为当前状态，value 为允许流转到的状态集合。
     * 已拒绝 / 已取消 / 已完成 是终态，集合为空。
     */
    private static final Map<BookingStatusEnum, Set<BookingStatusEnum>> TRANSITIONS = Map.of(
            WAIT_AUDIT, EnumSet.of(PASSED, REJECTED, CANCELED),
            PASSED, EnumSet.of(CHECKED_IN, CANCELED),
            REJECTED, EnumSet.noneOf(BookingStatusEnum.class),
            CANCELED, EnumSet.noneOf(BookingStatusEnum.class),
            CHECKED_IN, EnumSet.of(FINISHED),
            FINISHED, EnumSet.noneOf(BookingStatusEnum.class)
    );

    public static BookingStatusEnum of(Integer code) {
        BookingStatusEnum status = code == null ? null : CODE_MAP.get(code);
        if (status == null) {
            throw new BusinessException(ErrorCode.BOOKING_STATUS_ERROR);
        }
        return status;
    }

    /** 当前状态是否允许流转到 target */
    public boolean canTransferTo(BookingStatusEnum target) {
        return target != null && TRANSITIONS.get(this).contains(target);
    }

    /**
     * 校验状态流转，非法则抛业务异常。
     *
     * <p>提示里带上**具体状态名**（例如「你的预约当前为「已取消」，无法执行该操作」），
     * 比统一的「状态错误」更容易定位问题，前端也能直接展示给用户。</p>
     */
    public static void checkTransfer(Integer currentCode, BookingStatusEnum target) {
        BookingStatusEnum current = of(currentCode);
        if (!current.canTransferTo(target)) {
            throw new BusinessException(ErrorCode.BOOKING_STATUS_ERROR,
                    "你的预约当前为「" + current.getDesc() + "」，无法执行该操作");
        }
    }

    /** 是否为终态（不可再变更） */
    public boolean isFinal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    /**
     * 是否「占着名额」的状态。
     *
     * <p>已取消 / 已拒绝会把名额还回课程，因此不计入限约次数；
     * 待审核 / 已通过 / 已核销 / 已完成都还占着这个用户在这门课上的位置。</p>
     */
    public boolean isQuotaOccupied() {
        return this == WAIT_AUDIT || this == PASSED || this == CHECKED_IN || this == FINISHED;
    }
}
