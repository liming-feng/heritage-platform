package com.lmf.heritage.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 · 用户/匠人列表查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端用户列表查询条件")
public class AdminUserQueryDTO {

    @Schema(description = "角色筛选 1普通用户 2匠人 3管理员；不传表示全部")
    private Integer role;

    @Schema(description = "状态筛选 0禁用 1正常；不传表示全部")
    private Integer status;

    @Schema(description = "关键词：匹配用户名 / 昵称 / 手机号（匠人还会匹配真实姓名）")
    private String keyword;

    @Schema(description = "注册时间范围-起（含）")
    private LocalDateTime startTime;

    @Schema(description = "注册时间范围-止（含）")
    private LocalDateTime endTime;

    @Schema(description = "匠人资质审核状态筛选 0待审核 1已通过 2已驳回；仅 role=2 时有效")
    private Integer auditStatus;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 10")
    private Long pageSize = 10L;

    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }
        if (pageSize > 200) {
            pageSize = 200L;
        }
        // 空串必须转 null：空串进 LIKE 会变成 `%%`，把筛选条件悄悄吃掉
        if (keyword != null) {
            String t = keyword.trim();
            keyword = t.isEmpty() ? null : t;
        }
    }
}
