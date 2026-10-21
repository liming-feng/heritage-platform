package com.lmf.heritage.module.course.vo;

import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 课程详情
 *
 * <p>在列表项的基础上补上富文本详情、授课匠人卡片与课程评价统计。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "课程详情")
public class CourseDetailVO extends CourseListVO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "课程简介")
    private String description;

    @Schema(description = "课程详情（富文本）")
    private String detail;

    @Schema(description = "授课匠人简要信息")
    private CraftsmanSimpleVO craftsman;

    @Schema(description = "课程评价数")
    private Long reviewCount;

    @Schema(description = "课程平均评分")
    private Double avgScore;

    @Schema(description = "课程所需总时长文本，如 3 小时")
    private String durationText;
}
