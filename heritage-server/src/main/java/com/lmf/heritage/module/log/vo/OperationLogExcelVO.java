package com.lmf.heritage.module.log.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 操作日志导出行
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "操作日志导出行")
public class OperationLogExcelVO {

    @ExcelProperty("操作时间")
    @ColumnWidth(20)
    private String createTime;

    @ExcelProperty("操作人")
    @ColumnWidth(14)
    private String operator;

    @ExcelProperty("角色")
    @ColumnWidth(10)
    private String roleText;

    @ExcelProperty("操作类型")
    @ColumnWidth(18)
    private String operation;

    @ExcelProperty("请求方法")
    @ColumnWidth(24)
    private String method;

    @ExcelProperty("操作 IP")
    @ColumnWidth(16)
    private String ip;

    @ExcelProperty("耗时(ms)")
    @ColumnWidth(12)
    private Long costMs;

    @ExcelProperty("结果")
    @ColumnWidth(10)
    private String successText;

    @ExcelProperty("异常信息")
    @ColumnWidth(36)
    private String errorMsg;

    @ExcelProperty("请求参数")
    @ColumnWidth(60)
    private String params;
}
