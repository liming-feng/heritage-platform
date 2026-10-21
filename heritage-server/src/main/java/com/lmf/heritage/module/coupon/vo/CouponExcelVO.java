package com.lmf.heritage.module.coupon.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端 · 券种列表导出
 *
 * <p>数量类字段一律导出为数值型，运营拿到表要能直接排序、求和。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(16)
@Schema(description = "券种列表导出")
public class CouponExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("券ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("券名称")
    @ColumnWidth(26)
    private String name;

    @ExcelProperty("类型")
    @ColumnWidth(10)
    private String typeDesc;

    @ExcelProperty("适用范围")
    @ColumnWidth(12)
    private String scopeDesc;

    @ExcelProperty("优惠规则")
    @ColumnWidth(22)
    private String ruleText;

    @ExcelProperty("门槛金额")
    @ColumnWidth(12)
    private BigDecimal threshold;

    @ExcelProperty("发放总量")
    @ColumnWidth(12)
    private Integer totalCount;

    @ExcelProperty("已领取")
    @ColumnWidth(12)
    private Integer receivedCount;

    @ExcelProperty("已使用")
    @ColumnWidth(12)
    private Integer usedCount;

    @ExcelProperty("领取率(%)")
    @ColumnWidth(12)
    private BigDecimal receiveRate;

    @ExcelProperty("核销率(%)")
    @ColumnWidth(12)
    private BigDecimal useRate;

    @ExcelProperty("每人限领")
    @ColumnWidth(12)
    private Integer perLimit;

    @ExcelProperty("券状态")
    @ColumnWidth(10)
    private String statusDesc;

    @ExcelProperty("有效期状态")
    @ColumnWidth(14)
    private String effectiveDesc;

    @ExcelProperty("有效期起")
    @ColumnWidth(20)
    private String startTimeText;

    @ExcelProperty("有效期止")
    @ColumnWidth(20)
    private String endTimeText;
}
