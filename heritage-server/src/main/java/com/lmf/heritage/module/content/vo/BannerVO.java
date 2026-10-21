package com.lmf.heritage.module.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 轮播图展示对象
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "轮播图")
public class BannerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "轮播 id")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片 URL")
    private String image;

    @Schema(description = "跳转链接")
    private String linkUrl;
}
