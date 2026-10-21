package com.lmf.heritage.module.craftsman.vo;

import com.lmf.heritage.module.course.vo.CourseListVO;
import com.lmf.heritage.module.goods.vo.GoodsListVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 匠人主页（履历 / 师承 / 代表作 / 在售商品 / 开设课程）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人主页")
public class CraftsmanDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "匠人 id")
    private Long id;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "技艺门类")
    private String craftType;

    @Schema(description = "师承")
    private String inheritance;

    @Schema(description = "个人简介")
    private String intro;

    @Schema(description = "履历（富文本）")
    private String resume;

    @Schema(description = "主页封面图")
    private String cover;

    @Schema(description = "关注数")
    private Integer followerCount;

    @Schema(description = "在售作品数")
    private Long goodsCount;

    @Schema(description = "开设课程数")
    private Long courseCount;

    @Schema(description = "当前登录用户是否已关注（未登录恒为 false）")
    private Boolean followed;

    @Schema(description = "代表作 / 在售商品")
    private List<GoodsListVO> goodsList;

    @Schema(description = "开设的课程")
    private List<CourseListVO> courseList;

    @Schema(description = "入驻时间")
    private java.time.LocalDateTime joinTime;
}
