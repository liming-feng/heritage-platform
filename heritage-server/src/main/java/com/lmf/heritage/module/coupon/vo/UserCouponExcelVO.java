package com.lmf.heritage.module.coupon.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端 · 领取记录导出
 *
 * <p>刻意<b>不导出手机号</b>：这份表的用途是核对核销情况，
 * 用户名 + 订单号已经足够追溯到具体订单，而手机号属于个人信息。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(16)
@Schema(description = "领取记录导出")
public class UserCouponExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("记录ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("券名称")
    @ColumnWidth(26)
    private String couponName;

    @ExcelProperty("类型")
    @ColumnWidth(10)
    private String typeDesc;

    @ExcelProperty("优惠规则")
    @ColumnWidth(22)
    private String ruleText;

    @ExcelProperty("用户名")
    @ColumnWidth(16)
    private String username;

    @ExcelProperty("昵称")
    @ColumnWidth(16)
    private String nickname;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusDesc;

    @ExcelProperty("使用订单号")
    @ColumnWidth(22)
    private String orderNo;

    @ExcelProperty("领取时间")
    @ColumnWidth(20)
    private String createTimeText;

    @ExcelProperty("使用时间")
    @ColumnWidth(20)
    private String usedTimeText;

    @ExcelProperty("过期时间")
    @ColumnWidth(20)
    private String expireTimeText;
}
