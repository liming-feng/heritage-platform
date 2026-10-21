package com.lmf.heritage.module.craftsman.vo;

import com.lmf.heritage.module.craftsman.entity.Craftsman;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 匠人简要信息（列表、首页推荐、商品详情页的匠人卡片共用）
 *
 * <p>只暴露公开字段：idCardNo / certImage 这类资质材料在实体上已被 {@code @JsonIgnore} 挡掉，
 * 这里也不复制，从源头保证不泄露。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人简要信息")
public class CraftsmanSimpleVO implements Serializable {

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

    @Schema(description = "主页封面图")
    private String cover;

    @Schema(description = "关注数")
    private Integer followerCount;

    @Schema(description = "在售作品数")
    private Integer goodsCount;

    public static CraftsmanSimpleVO from(Craftsman c) {
        if (c == null) {
            return null;
        }
        CraftsmanSimpleVO vo = new CraftsmanSimpleVO();
        vo.setId(c.getId());
        vo.setRealName(c.getRealName());
        vo.setCraftType(c.getCraftType());
        vo.setInheritance(c.getInheritance());
        vo.setIntro(c.getIntro());
        vo.setCover(c.getCover());
        vo.setFollowerCount(c.getFollowerCount());
        return vo;
    }
}
