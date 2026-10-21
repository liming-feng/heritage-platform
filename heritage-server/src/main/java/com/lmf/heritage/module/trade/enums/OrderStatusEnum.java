package com.lmf.heritage.module.trade.enums;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机
 *
 * <pre>
 *  待付款(0) ──支付──→ 待发货(1) ──匠人发货──→ 待收货(2) ──确认收货──→ 已完成(3)
 *      │                    │                       │
 *      └──取消──→ 已取消(5)  └────退款同意────→ 已退款(4) ←────退款同意────┘
 * </pre>
 *
 * <p>所有状态变更都必须经过 {@link #checkTransfer(Integer, Integer)}，
 * 非法跳转直接抛业务异常，避免在业务代码里散落 if-else 判断。</p>
 *
 * @author 李明峰 23100025
 */
@Getter
public enum OrderStatusEnum {

    WAIT_PAY(0, "待付款"),
    WAIT_DELIVER(1, "待发货"),
    WAIT_RECEIVE(2, "待收货"),
    FINISHED(3, "已完成"),
    REFUNDED(4, "已退款"),
    CANCELED(5, "已取消");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private static final Map<Integer, OrderStatusEnum> CODE_MAP = Arrays.stream(values())
            .collect(java.util.stream.Collectors.toMap(OrderStatusEnum::getCode, e -> e));

    /**
     * 状态流转表：key 为当前状态，value 为允许流转到的状态集合。
     * 已完成 / 已退款 / 已取消是终态，集合为空。
     */
    private static final Map<OrderStatusEnum, Set<OrderStatusEnum>> TRANSITIONS = Map.of(
            WAIT_PAY, EnumSet.of(WAIT_DELIVER, CANCELED),
            WAIT_DELIVER, EnumSet.of(WAIT_RECEIVE, REFUNDED),
            WAIT_RECEIVE, EnumSet.of(FINISHED, REFUNDED),
            FINISHED, EnumSet.noneOf(OrderStatusEnum.class),
            REFUNDED, EnumSet.noneOf(OrderStatusEnum.class),
            CANCELED, EnumSet.noneOf(OrderStatusEnum.class)
    );

    public static OrderStatusEnum of(Integer code) {
        OrderStatusEnum status = code == null ? null : CODE_MAP.get(code);
        if (status == null) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        return status;
    }

    /** 当前状态是否允许流转到 target */
    public boolean canTransferTo(OrderStatusEnum target) {
        return target != null && TRANSITIONS.get(this).contains(target);
    }

    /**
     * 校验状态流转，非法则抛业务异常（提示里带上具体状态名，方便定位问题）
     */
    public static void checkTransfer(Integer currentCode, OrderStatusEnum target) {
        OrderStatusEnum current = of(currentCode);
        if (!current.canTransferTo(target)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR,
                    "订单当前为「" + current.getDesc() + "」，无法执行该操作");
        }
    }

    /** 是否为终态（不可再变更） */
    public boolean isFinal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
