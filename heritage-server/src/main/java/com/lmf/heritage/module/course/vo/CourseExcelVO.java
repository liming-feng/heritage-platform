package com.lmf.heritage.module.course.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 · 课程列表导出
 *
 * <p>日期导出为 {@code yyyy-MM-dd HH:mm:ss} 字符串而不是 LocalDateTime：
 * EasyExcel 对日期类型的默认格式受单元格样式影响，不同的 Excel 版本打开出来不一样。
 * 定成字符串，谁打开都是同一个样子。价格与名额保持数值型，方便直接统计。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(16)
@Schema(description = "课程列表导出")
public class CourseExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("课程ID")
    private Long id;

    @ExcelProperty("课程标题")
    @ColumnWidth(30)
    private String title;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("匠人")
    private String craftsmanName;

    @ExcelProperty("技艺门类")
    private String craftType;

    @ExcelProperty("价格")
    @ColumnWidth(10)
    private BigDecimal price;

    @ExcelProperty("上课地点")
    @ColumnWidth(24)
    private String address;

    @ExcelProperty("开课时间")
    @ColumnWidth(20)
    private LocalDateTime startTime;

    @ExcelProperty("结束时间")
    @ColumnWidth(20)
    private LocalDateTime endTime;

    @ExcelProperty("总名额")
    @ColumnWidth(10)
    private Integer totalQuota;

    @ExcelProperty("剩余名额")
    @ColumnWidth(10)
    private Integer remainQuota;

    @ExcelProperty("已约名额")
    @ColumnWidth(10)
    private Integer bookedQuota;

    @ExcelProperty("课程状态")
    private String statusDesc;

    @ExcelProperty("审核状态")
    private String auditStatusDesc;

    @ExcelProperty("驳回/下架原因")
    @ColumnWidth(28)
    private String rejectReason;

    @ExcelProperty("提交时间")
    @ColumnWidth(20)
    private String createTimeText;
}
