package com.lmf.heritage.module.craftsman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 · 匠人列表查询条件
 *
 * <p>与用户列表（{@code AdminUserQueryDTO}）刻意分开：两个列表筛选维度不同 ——
 * 用户看的是「注册时间、启停状态」，匠人看的是「资质审核状态、技艺门类、作品数」。
 * 合并成一个 DTO 会让两边各背一半用不到的字段，参数校验也说不清。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "管理端匠人列表查询条件")
public class AdminCraftsmanQueryDTO {

    @Schema(description = "资质审核状态 0待审核 1已通过 2已驳回；不传表示全部")
    private Integer auditStatus;

    @Schema(description = "技艺门类，如 剪纸 / 木雕")
    private String craftType;

    @Schema(description = "账号状态 0禁用 1正常（取自关联的 user 表）")
    private Integer status;

    @Schema(description = "关键词：匹配真实姓名 / 手机号 / 用户名 / 技艺门类")
    private String keyword;

    @Schema(description = "申请时间范围-起（含）")
    private LocalDateTime startTime;

    @Schema(description = "申请时间范围-止（含）")
    private LocalDateTime endTime;

    @Schema(description = "排序 follower关注数 / goods作品数 / new最新申请 / audit 待审优先；默认 audit")
    private String sort;

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
        if (keyword != null) {
            String t = keyword.trim();
            keyword = t.isEmpty() ? null : t;
        }
        if (craftType != null) {
            String t = craftType.trim();
            craftType = t.isEmpty() ? null : t;
        }
        if (sort == null || sort.isBlank()) {
            sort = "audit";
        }
    }
}
