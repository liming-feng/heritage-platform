package com.lmf.heritage.module.course.enums;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import lombok.Getter;

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
}
