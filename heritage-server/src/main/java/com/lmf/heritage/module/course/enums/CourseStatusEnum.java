package com.lmf.heritage.module.course.enums;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程状态（0未开始 1进行中 2已结束）
 *
 * <p>由定时任务按 {@code start_time} / {@code end_time} 自动推进，
 * 而不是由前端传参决定——课程到底「开没开课」是一个事实，不该由调用方声明。</p>
 *
 * @author 李明峰 23100025
 */
@Getter
public enum CourseStatusEnum {

    NOT_STARTED(0, "未开始"),
    ONGOING(1, "进行中"),
    FINISHED(2, "已结束");

    private final int code;
    private final String desc;

    CourseStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private static final Map<Integer, CourseStatusEnum> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(CourseStatusEnum::getCode, e -> e));

    public static CourseStatusEnum of(Integer code) {
        CourseStatusEnum status = code == null ? null : CODE_MAP.get(code);
        if (status == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND, "课程状态异常");
        }
        return status;
    }

    public static String descOf(Integer code) {
        CourseStatusEnum e = code == null ? null : CODE_MAP.get(code);
        return e == null ? "" : e.getDesc();
    }

    /**
     * 按时间判定某一时刻应处的状态（区间语义 [start, end)：到结束时刻即视为已结束）。
     *
     * <p>「状态由定时任务推进」是对的，但<strong>不能只有定时任务</strong>：
     * 每 10 分钟纠偏一次，意味着用户改完开课时间立刻刷新，最多会看到 10 分钟的错误状态
     * （把开课时间改成今天上午，列表还写着「未开始」）。所以写入/审核时都先算一次，
     * 定时任务退化为兜底（停机、改系统时间、批量导入）。</p>
     *
     * @param start 开课时间，可为空（视为未开始）
     * @param end   结束时间，可为空（视为未结束）
     * @param now   参照时刻
     */
    public static CourseStatusEnum at(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (end != null && !now.isBefore(end)) {
            return FINISHED;
        }
        if (start != null && !now.isBefore(start)) {
            return ONGOING;
        }
        return NOT_STARTED;
    }
}
