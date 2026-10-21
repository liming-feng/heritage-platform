package com.lmf.heritage.module.goods.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端 · 商品列表导出
 *
 * <p>价格与库存导出为<b>数值型</b>而不是字符串：运营拿到表第一件事通常是排序、
 * 求和、筛出「库存低于多少」的商品，文本型单元格会让这些操作全部失效
 * （Excel 会提示「此列中的数字为文本」）。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(16)
@Schema(description = "商品列表导出")
public class GoodsExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("商品ID")
    private Long id;

    @ExcelProperty("商品名称")
    @ColumnWidth(28)
    private String name;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("匠人")
    private String craftsmanName;

    @ExcelProperty("技艺门类")
    private String craftType;

    @ExcelProperty("售价")
    @ColumnWidth(12)
    private BigDecimal price;

    @ExcelProperty("原价")
    @ColumnWidth(12)
    private BigDecimal originalPrice;

    @ExcelProperty("库存")
    @ColumnWidth(10)
    private Integer stock;

    @ExcelProperty("销量")
    @ColumnWidth(10)
    private Integer sales;

    @ExcelProperty("浏览量")
    @ColumnWidth(10)
    private Integer viewCount;

    @ExcelProperty("商品状态")
    private String statusDesc;

    @ExcelProperty("审核状态")
    private String auditStatusDesc;

    @ExcelProperty("驳回/下架原因")
    @ColumnWidth(28)
    private String rejectReason;

    @ExcelProperty("发布时间")
    @ColumnWidth(22)
    private String createTimeText;
}
