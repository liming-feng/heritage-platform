package com.lmf.heritage.module.craftsman.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端 · 匠人资质审核详情
 *
 * <p>这是全平台<b>唯一</b>会返回证件号与资质图的接口。匠人实体上这两个字段标了
 * {@code @JsonIgnore}，所以只能在这里显式地从实体里取出来塞进 VO —— 这个「显式」
 * 就是安全边界：只要没人往 VO 上写，它们就不会出现在任何前台响应里。</p>
 *
 * <p>附带的 {@code goodsPreview} / {@code coursePreview} 是审核时的判断依据：
 * 光看一张证件图很难判断申请人是否真在做这门手艺，看他提交了什么作品更实际。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "匠人资质审核详情")
public class CraftsmanAuditDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "匠人 id")
    private Long id;

    @Schema(description = "关联用户 id")
    private Long userId;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "证件号（仅管理端可见）")
    private String idCardNo;

    @Schema(description = "资质证明图 URL，多张以逗号分隔（仅管理端可见）")
    private String certImage;

    @Schema(description = "资质证明图列表（certImage 拆分后的结果，前端直接遍历）")
    private List<String> certImages;

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

    @Schema(description = "资质审核状态 0待审核 1已通过 2已驳回")
    private Integer auditStatus;

    @Schema(description = "审核状态文案")
    private String auditStatusText;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "账号状态 0禁用 1正常")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime registerTime;

    @Schema(description = "申请时间")
    private LocalDateTime createTime;

    @Schema(description = "关注数")
    private Integer followerCount;

    @Schema(description = "作品预览：该匠人最近提交的 5 件商品")
    private List<PreviewItem> goodsPreview;

    @Schema(description = "课程预览：该匠人最近创建的 5 门课程")
    private List<PreviewItem> coursePreview;

    /**
     * 审核详情里的「作品/课程」缩略项
     *
     * <p>只取判断资质够不够所需的最小字段，不返回价格库存等交易信息 ——
     * 审核关注的是「他是不是真在做这门手艺」。</p>
     */
    @Data
    @Schema(description = "作品/课程预览项")
    public static class PreviewItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "id")
        private Long id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "封面图")
        private String cover;

        @Schema(description = "副标题：商品为分类名，课程为开课时间")
        private String extra;

        @Schema(description = "状态文案")
        private String statusText;
    }
}
