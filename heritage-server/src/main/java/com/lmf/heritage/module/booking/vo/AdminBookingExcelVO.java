package com.lmf.heritage.module.booking.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端 · 全平台预约导出
 *
 * <p>名额数与联系电话一并导出：拿到这张表的场景通常是「某门课要改期，
 * 得挨个通知报名的人」，这时候缺了电话就得再回系统里翻一遍。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(16)
@Schema(description = "预约列表导出")
public class AdminBookingExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("预约单号")
    @ColumnWidth(22)
    private String bookingNo;

    @ExcelProperty("课程标题")
    @ColumnWidth(30)
    private String courseTitle;

    @ExcelProperty("匠人")
    private String craftsmanName;

    @ExcelProperty("预约人")
    private String contactName;

    @ExcelProperty("联系电话")
    @ColumnWidth(14)
    private String contactPhone;

    @ExcelProperty("名额")
    @ColumnWidth(8)
    private Integer quota;

    @ExcelProperty("状态")
    private String statusDesc;

    @ExcelProperty("核销码")
    @ColumnWidth(14)
    private String checkinCode;

    @ExcelProperty("开课时间")
    @ColumnWidth(20)
    private String courseStartTimeText;

    @ExcelProperty("提交时间")
    @ColumnWidth(20)
    private String createTimeText;

    @ExcelProperty("备注")
    @ColumnWidth(24)
    private String remark;
}
