package com.lmf.heritage.module.booking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端 · 预约统计的 5 组结果
 *
 * <p>5 个统计放在同一个文件里用嵌套类表达，而不是拆成 5 个顶层 VO：
 * 它们是同一件事（预约监管）的 5 个侧面，口径彼此参照 ——
 * 比如「审核漏斗」里的核销数与「爽约率」里的到场数必须同源。
 * 摆在一起，改口径时不容易只改一半。</p>
 *
 * <p><b>共同口径</b>（与匠人看板 P7 完全一致，两个后台的数字不能打架）：<br>
 * 已支付/有效预约 = status IN (0,1,4,5)；<br>
 * 占用名额 = 上述状态记录的 quota 之和；<br>
 * 到场 = status IN (4,5)（已核销 + 已结课）；<br>
 * 爽约 / 应到未到 = status = 1（已通过但没来核销）。</p>
 *
 * @author 李明峰 23100025
 */
public final class AdminBookingStatVO {

    private AdminBookingStatVO() {
    }

    /**
     * ① 时段分布（按课程开课时间落在哪个时段统计预约单）
     *
     * <p>看的是「平台把课排在什么时候、用户在什么时段来得多」，
     * 为排期与场地安排提供依据。四个时段固定返回，没有的补 0 ——
     * 柱状图不该因为某个时段没数据就少一根柱子。</p>
     */
    @Data
    @Schema(description = "预约时段分布")
    public static class SlotDistribution implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "预约单总数")
        private Long total;

        @Schema(description = "参与统计的名额总数")
        private Long totalQuota;

        @Schema(description = "四个时段（固定顺序：上午/下午/晚上/凌晨）")
        private List<Slot> slots;

        @Data
        @Schema(description = "单个时段")
        public static class Slot implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            @Schema(description = "时段编码 morning/afternoon/evening/night")
            private String slot;

            @Schema(description = "时段名称")
            private String slotName;

            @Schema(description = "时间范围说明，如 06:00-12:00")
            private String range;

            @Schema(description = "预约单数")
            private Long bookingCount;

            @Schema(description = "其中已通过 / 已核销 / 已完成的单数")
            private Long passedCount;

            @Schema(description = "占总数的百分比，保留 1 位小数")
            private Double percent;
        }
    }

    /**
     * ② 爽约率
     *
     * <p><b>只在已结束的课程上算</b>：课程还没开始，「应到未到」这个说法不成立，
     * 把未开课的预约算进来会把爽约率稀释成一个没有意义的数字。<br>
     * 没有任何已结束课程时，{@code absentRate} 返回 {@code null} 而不是 0 ——
     * 「没数据」和「爽约率 0%」不是一回事。</p>
     */
    @Data
    @Schema(description = "爽约率统计")
    public static class AbsentRate implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "已结束课程上的有效预约总数（分母）")
        private Long total;

        @Schema(description = "实到人次（已核销 + 已结课）")
        private Long attend;

        @Schema(description = "应到未到人次（已通过但未核销）")
        private Long absent;

        @Schema(description = "爽约率（%），无数据时为 null")
        private Double absentRate;

        @Schema(description = "已结束课程数")
        private Long endedCourseCount;

        @Schema(description = "爽约人次最多的课程（最多 10 门，爽约数为 0 的不返回）")
        private List<CourseAbsent> courses;

        @Data
        @Schema(description = "单门课程的爽约情况")
        public static class CourseAbsent implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            @Schema(description = "课程 id")
            private Long courseId;

            @Schema(description = "课程标题")
            private String courseTitle;

            @Schema(description = "匠人姓名")
            private String craftsmanName;

            @Schema(description = "有效预约数")
            private Long total;

            @Schema(description = "实到人次")
            private Long attend;

            @Schema(description = "应到未到人次")
            private Long absent;

            @Schema(description = "该课程爽约率（%）")
            private Double absentRate;
        }
    }

    /**
     * ③ 审核转化漏斗：提交 → 通过 → 到场
     *
     * <p>三段的差值是两个不同的流失原因：<br>
     * 提交→通过 的缺口 = 被匠人拒绝 + 超时自动拒绝 + 用户取消；<br>
     * 通过→到场 的缺口 = 爽约。<br>
     * 所以漏斗里除了三段数字，还给出「被拒 / 取消」的明细，否则缺口无法解释。</p>
     */
    @Data
    @Schema(description = "预约审核转化漏斗")
    public static class AuditFunnel implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "提交总数")
        private Long submitted;

        @Schema(description = "审核通过数（含后续核销、结课）")
        private Long passed;

        @Schema(description = "到场数（已核销 + 已结课）")
        private Long checkedIn;

        @Schema(description = "待审核数")
        private Long waitAudit;

        @Schema(description = "已拒绝数（匠人拒绝 + 系统超时）")
        private Long rejected;

        @Schema(description = "已取消数")
        private Long canceled;

        @Schema(description = "通过率（%）= 通过 / 提交")
        private Double passRate;

        @Schema(description = "到场率（%）= 到场 / 通过")
        private Double attendRate;

        @Schema(description = "整体转化率（%）= 到场 / 提交")
        private Double overallRate;

        @Schema(description = "漏斗三段，便于前端直接画图")
        private List<Stage> stages;

        @Data
        @Schema(description = "漏斗单个阶段")
        public static class Stage implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            @Schema(description = "阶段名")
            private String name;

            @Schema(description = "数量")
            private Long value;

            @Schema(description = "相对提交数的比例（%）")
            private Double percent;
        }
    }

    /**
     * ④ 课程热度榜
     *
     * <p>排序主键是「占用名额之和」而不是「预约单数」：一张单可能约 3 个名额，
     * 按单数排会把「人多但单少」的课排到后面。这一点与匠人看板的课程热度榜
     * 用的是同一个定义。</p>
     */
    @Data
    @Schema(description = "课程热度榜")
    public static class CourseHeat implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "统计范围：全部课程")
        private String scope;

        @Schema(description = "榜单")
        private List<Item> list;

        @Data
        @Schema(description = "热度榜单项")
        public static class Item implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            @Schema(description = "排名，从 1 开始")
            private Integer rank;

            @Schema(description = "课程 id")
            private Long courseId;

            @Schema(description = "课程标题")
            private String courseTitle;

            @Schema(description = "封面")
            private String courseCover;

            @Schema(description = "匠人姓名")
            private String craftsmanName;

            @Schema(description = "开课时间")
            private LocalDateTime startTime;

            @Schema(description = "课程状态 0未开始 1进行中 2已结束")
            private Integer status;

            @Schema(description = "状态文案")
            private String statusDesc;

            @Schema(description = "预约单数")
            private Long bookingCount;

            @Schema(description = "占用名额数（排序主键）")
            private Long bookingPeople;

            @Schema(description = "总名额")
            private Integer totalQuota;

            @Schema(description = "剩余名额")
            private Integer remainQuota;

            @Schema(description = "名额使用率（%）")
            private Double usedRate;
        }
    }

    /**
     * ⑤ 近 N 天预约趋势
     *
     * <p>SQL 只返回有预约的日期，缺口由服务层补齐成连续日期 ——
     * 理由与匠人看板一致：日期轴缺格会把折线图画成一条错误的斜线。</p>
     */
    @Data
    @Schema(description = "预约趋势")
    public static class Trend implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "统计天数")
        private Integer days;

        @Schema(description = "起始日期 yyyy-MM-dd")
        private String startDate;

        @Schema(description = "结束日期 yyyy-MM-dd")
        private String endDate;

        @Schema(description = "区间内提交总数")
        private Long totalCount;

        @Schema(description = "区间内名额总数")
        private Long totalQuota;

        @Schema(description = "日均提交数，保留 1 位小数")
        private Double avgCount;

        @Schema(description = "单日最高提交数")
        private Long maxCount;

        @Schema(description = "每日数据点，日期连续")
        private List<DayPoint> points;

        @Data
        @Schema(description = "单日数据点")
        public static class DayPoint implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            @Schema(description = "日期 yyyy-MM-dd")
            private String date;

            @Schema(description = "短日期 MM-dd")
            private String dateText;

            @Schema(description = "星期几")
            private String weekday;

            @Schema(description = "提交单数")
            private Long count;

            @Schema(description = "其中已通过 / 已核销 / 已完成的单数")
            private Long passedCount;

            @Schema(description = "名额数")
            private Long quota;

            @Schema(description = "当天是否有数据")
            private Boolean hasData;
        }
    }
}
