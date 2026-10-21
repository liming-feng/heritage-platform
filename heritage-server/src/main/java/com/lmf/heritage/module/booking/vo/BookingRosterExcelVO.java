package com.lmf.heritage.module.booking.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报名名单导出行（匠人端导出 Excel 用）
 *
 * <p>单独建一个导出 VO 而不是直接导 {@code BookingVO}：导出的是给线下签到用的纸质名单，
 * 只要「谁、几个人、电话、状态」这些信息，把整个 VO 导出去反而是一堆没人看的列。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "报名名单导出行")
public class BookingRosterExcelVO {

    @ExcelProperty("预约单号")
    @ColumnWidth(20)
    private String bookingNo;

    @ExcelProperty("预约人")
    @ColumnWidth(14)
    private String contactName;

    @ExcelProperty("联系电话")
    @ColumnWidth(16)
    private String contactPhone;

    @ExcelProperty("名额")
    @ColumnWidth(8)
    private Integer quota;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusDesc;

    @ExcelProperty("核销码")
    @ColumnWidth(14)
    private String checkinCode;

    @ExcelProperty("签到时间")
    @ColumnWidth(20)
    private String checkinTime;

    @ExcelProperty("提交时间")
    @ColumnWidth(20)
    private String createTime;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;
}
