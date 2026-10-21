package com.lmf.heritage.module.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端 · 商品审核详情
 *
 * <p>比前台详情多三样审核必需而前台无关的东西：<br>
 * ① 匠人的<b>联系方式</b> —— 图文不符时审核员要先去问一句，而不是直接驳回；<br>
 * ② <b>全部</b>图片（含待审核状态下前台看不到的）；<br>
 * ③ 历史<b>驳回原因</b> —— 重复提交的商品，审核员需要知道上次为什么被退。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端商品审核详情")
public class AdminGoodsDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品 id")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "卖点描述")
    private String description;

    @Schema(description = "详情富文本")
    private String detail;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "是否纯手工 0否 1是")
    private Integer isHandmade;

    @Schema(description = "图片列表（含细节图）")
    private List<String> images;

    @Schema(description = "规格列表")
    private List<GoodsSpecVO> specs;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "分类名")
    private String categoryName;

    @Schema(description = "匠人 id")
    private Long craftsmanId;

    @Schema(description = "匠人姓名")
    private String craftsmanName;

    @Schema(description = "技艺门类")
    private String craftType;

    @Schema(description = "匠人联系电话（审核沟通用）")
    private String craftsmanPhone;

    @Schema(description = "商品状态 0下架 1在售 2待审核")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusDesc;

    @Schema(description = "审核状态 0待审核 1已通过 2已驳回")
    private Integer auditStatus;

    @Schema(description = "审核状态文案")
    private String auditStatusDesc;

    @Schema(description = "驳回 / 下架原因")
    private String rejectReason;

    @Schema(description = "评分均值（无评价为 null）")
    private Double ratingAvg;

    @Schema(description = "评价数")
    private Integer reviewCount;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
