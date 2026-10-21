package com.lmf.heritage.module.content.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端 · 轮播图导出
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(18)
@Schema(description = "轮播图导出")
public class BannerExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("轮播ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("标题")
    @ColumnWidth(24)
    private String title;

    @ExcelProperty("图片地址")
    @ColumnWidth(34)
    private String image;

    @ExcelProperty("跳转链接")
    @ColumnWidth(26)
    private String linkUrl;

    @ExcelProperty("位置")
    @ColumnWidth(12)
    private String positionDesc;

    @ExcelProperty("排序")
    @ColumnWidth(8)
    private Integer sort;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusDesc;

    @ExcelProperty("创建时间")
    @ColumnWidth(22)
    private String createTimeText;
}
