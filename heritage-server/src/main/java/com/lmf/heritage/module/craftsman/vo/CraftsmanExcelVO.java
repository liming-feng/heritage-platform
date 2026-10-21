package com.lmf.heritage.module.craftsman.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端 · 匠人列表导出
 *
 * <p>导出列与页面列保持一致（页面没有的「联系电话」在导出里保留 —— 线下核实资质时要用），
 * 同样<b>不含</b>证件号：导出的 Excel 会被转发、会被留存，证件号一旦流出就无法收回，
 * 需要核验时走审核详情页看。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(18)
@Schema(description = "匠人列表导出")
public class CraftsmanExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("匠人ID")
    private Long id;

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("真实姓名")
    private String realName;

    @ExcelProperty("联系电话")
    private String phone;

    @ExcelProperty("技艺门类")
    private String craftType;

    @ExcelProperty("师承")
    @ColumnWidth(24)
    private String inheritance;

    @ExcelProperty("关注数")
    private Integer followerCount;

    @ExcelProperty("在售作品")
    private Integer goodsCount;

    @ExcelProperty("已开课")
    private Integer courseCount;

    @ExcelProperty("审核状态")
    private String auditStatusText;

    @ExcelProperty("驳回原因")
    @ColumnWidth(28)
    private String rejectReason;

    @ExcelProperty("账号状态")
    private String statusText;

    @ExcelProperty("申请时间")
    @ColumnWidth(22)
    private String createTimeText;
}
