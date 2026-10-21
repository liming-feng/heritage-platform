package com.lmf.heritage.module.trade.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单导出行（匠人端导出 Excel 用）
 *
 * <p>列的取舍是照「对账与发货」两个用途来的：对账要金额三件套（商品总额 / 优惠 / 实付），
 * 发货要收货信息与物流信息。商品的图片、规格 id 这类只对前台有意义的数据一概不导。</p>
 *
 * <p>金额用 {@code BigDecimal} 而不是格式化后的字符串：Excel 里应当是可参与计算的数字，
 * 导出成「¥299.00」这种字符串就没法直接求和了。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "订单导出行")
public class OrderExcelVO {

    @ExcelProperty("订单号")
    @ColumnWidth(22)
    private String orderNo;

    @ExcelProperty("下单时间")
    @ColumnWidth(20)
    private String createTime;

    @ExcelProperty("买家")
    @ColumnWidth(14)
    private String buyerName;

    @ExcelProperty("收货人")
    @ColumnWidth(14)
    private String receiver;

    @ExcelProperty("收货电话")
    @ColumnWidth(16)
    private String receiverPhone;

    @ExcelProperty("收货地址")
    @ColumnWidth(40)
    private String addressSnapshot;

    @ExcelProperty("商品明细")
    @ColumnWidth(40)
    private String itemSummary;

    @ExcelProperty("件数")
    @ColumnWidth(8)
    private Integer totalQuantity;

    @ExcelProperty("商品总额")
    @ColumnWidth(12)
    private BigDecimal totalAmount;

    @ExcelProperty("优惠金额")
    @ColumnWidth(12)
    private BigDecimal discountAmount;

    @ExcelProperty("实付金额")
    @ColumnWidth(12)
    private BigDecimal payAmount;

    @ExcelProperty("订单状态")
    @ColumnWidth(12)
    private String statusDesc;

    @ExcelProperty("物流公司")
    @ColumnWidth(16)
    private String logisticsCompany;

    @ExcelProperty("物流单号")
    @ColumnWidth(22)
    private String logisticsNo;

    @ExcelProperty("发货时间")
    @ColumnWidth(20)
    private String deliveryTime;

    @ExcelProperty("买家备注")
    @ColumnWidth(24)
    private String remark;

    @ExcelProperty("匠人备注")
    @ColumnWidth(24)
    private String craftsmanRemark;
}
