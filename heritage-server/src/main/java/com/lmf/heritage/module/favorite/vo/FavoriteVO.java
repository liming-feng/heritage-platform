package com.lmf.heritage.module.favorite.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的收藏项
 *
 * <p>把商品的名称/封面/价格、课程的标题/封面/费用、资讯的标题/封面统一映射到
 * 同一组字段上，前端一个列表组件就能渲染三种类型。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "我的收藏项")
public class FavoriteVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "收藏记录 id")
    private Long id;

    @Schema(description = "目标类型 1商品 2课程 3资讯")
    private Integer targetType;

    @Schema(description = "目标类型描述")
    private String targetTypeDesc;

    @Schema(description = "目标 id")
    private Long targetId;

    @Schema(description = "名称（商品名 / 课程标题 / 资讯标题）")
    private String title;

    @Schema(description = "副标题或摘要")
    private String subtitle;

    @Schema(description = "封面图")
    private String cover;

    @Schema(description = "价格（商品售价 / 课程费用；资讯为 null）")
    private BigDecimal price;

    @Schema(description = "目标是否仍然可见（已下架 / 已删除时为 false，前端置灰）")
    private Boolean targetAvailable;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;
}
