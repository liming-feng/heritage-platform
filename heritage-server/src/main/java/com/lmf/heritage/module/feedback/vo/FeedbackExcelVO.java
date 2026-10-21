package com.lmf.heritage.module.feedback.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端 · 留言反馈导出
 *
 * <p>刻意<b>不导出联系方式</b>：反馈内容已经包含用户自己写的联系方式，
 * 单独再导出 contact 列等于把个人信息平铺进一份会到处流传的文件里。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(16)
@Schema(description = "留言反馈导出")
public class FeedbackExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("反馈ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("提交人")
    @ColumnWidth(16)
    private String username;

    @ExcelProperty("昵称")
    @ColumnWidth(16)
    private String nickname;

    @ExcelProperty("类型")
    @ColumnWidth(12)
    private String typeDesc;

    @ExcelProperty("反馈内容")
    @ColumnWidth(46)
    private String content;

    @ExcelProperty("管理员回复")
    @ColumnWidth(40)
    private String reply;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusDesc;

    @ExcelProperty("提交时间")
    @ColumnWidth(20)
    private String createTimeText;

    @ExcelProperty("处理完成时间")
    @ColumnWidth(20)
    private String handleTimeText;

    @ExcelProperty("处理时长(小时)")
    @ColumnWidth(14)
    private BigDecimal handleHours;
}
