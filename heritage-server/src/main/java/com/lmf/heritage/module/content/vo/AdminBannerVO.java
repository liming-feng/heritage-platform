package com.lmf.heritage.module.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端 · 轮播图列表项
 *
 * <p>比用户端 {@code BannerVO} 多出位置、排序、状态三个字段 ——
 * 这三个字段正是运营要维护的东西，管理端看不见就没法维护。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端轮播图")
public class AdminBannerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片 URL")
    private String image;

    @Schema(description = "跳转链接")
    private String linkUrl;

    @Schema(description = "位置 1首页 2课程页")
    private Integer position;

    @Schema(description = "位置描述，如「首页」")
    private String positionDesc;

    @Schema(description = "排序号，越小越前")
    private Integer sort;

    @Schema(description = "状态 0停用 1启用")
    private Integer status;

    @Schema(description = "状态描述，如「启用」/「停用」")
    private String statusDesc;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后编辑时间")
    private LocalDateTime updateTime;
}
