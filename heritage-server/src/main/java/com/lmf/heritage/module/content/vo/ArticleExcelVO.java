package com.lmf.heritage.module.content.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端 · 资讯列表导出
 *
 * <p>浏览量 / 点赞数 / 评论数导出为<b>数值型</b>：这三个字段是判断
 * 「哪篇内容值得继续投入」的依据，导出后要能直接排序。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@ColumnWidth(16)
@Schema(description = "资讯列表导出")
public class ArticleExcelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("资讯ID")
    private Long id;

    @ExcelProperty("标题")
    @ColumnWidth(34)
    private String title;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("作者")
    @ColumnWidth(14)
    private String author;

    @ExcelProperty("标签")
    @ColumnWidth(20)
    private String tags;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusDesc;

    @ExcelProperty("是否置顶")
    @ColumnWidth(10)
    private String isTopDesc;

    @ExcelProperty("浏览量")
    @ColumnWidth(10)
    private Integer viewCount;

    @ExcelProperty("点赞数")
    @ColumnWidth(10)
    private Integer likeCount;

    @ExcelProperty("评论数")
    @ColumnWidth(10)
    private Integer commentCount;

    @ExcelProperty("发布时间")
    @ColumnWidth(22)
    private String createTimeText;
}
