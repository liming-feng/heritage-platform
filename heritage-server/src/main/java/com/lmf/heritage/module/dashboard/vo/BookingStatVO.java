package com.lmf.heritage.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 预约专属指标（本课题题眼在看板上的落脚点）
 *
 * <p><b>核销率是怎么定义的</b>——这个指标最容易各说各话，所以口径写在最前面：</p>
 * <blockquote>
 *   核销率 = 已结束课程上的已核销人次 ÷（已核销人次 + 应到未到人次）
 * </blockquote>
 * <ul>
 *   <li><b>只在「已结束」的课程上算</b>。还没开课的课程里那些「已通过未核销」是正常状态
 *       （人还没来怎么会核销），把它们算成未到场会把核销率毫无道理地压低。</li>
 *   <li><b>按人次（quota）而不是按单数算</b>。一张预约带 3 个人来，这 3 个人都到场了才叫核销了 3 个名额。</li>
 *   <li><b>应到未到 = 课程已结束、状态却还停在「已通过」的预约</b>。课程都结束了还没核销，
 *       就是实际没来——这也是「爽约率」的另一面（爽约率 = 100% − 核销率）。</li>
 * </ul>
 *
 * <p>同时返回 {@code waitAudit}（待审核单数）与 {@code passedUnchecked}（全平台范围内
 * 已通过未核销的单数）：前者是匠人当下的待办量，后者包含尚未开课的正常预约，
 * 两者不重叠、也不冲突，看板上分别展示即可。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "预约专属指标")
public class BookingStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "待审核预约单数（含未超时与已超时）")
    private Long waitAudit;

    @Schema(description = "已通过未核销单数（全部课程口径，含尚未开课的正常预约）")
    private Long passedUnchecked;

    @Schema(description = "已核销单数（含已核销与已完成）")
    private Long checkedCount;

    @Schema(description = "有效预约单数（排除已拒绝与已取消）")
    private Long validCount;

    @Schema(description = "已结束课程数")
    private Long endedCourseCount;

    @Schema(description = "实到人次（已结束课程上已核销 / 已完成的名额之和）")
    private Long checkinPeople;

    @Schema(description = "应到未到人次（已结束课程上仍为已通过的名额之和，即爽约）")
    private Long absentPeople;

    @Schema(description = "核销率（0-100，1 位小数）；没有已结束课程时为 null，避免除零显示成 0%")
    private Double checkinRate;

    @Schema(description = "各课程名额使用率")
    private List<CourseQuotaVO> courses;
}
