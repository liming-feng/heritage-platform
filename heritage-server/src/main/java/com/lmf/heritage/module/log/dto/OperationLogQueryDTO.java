package com.lmf.heritage.module.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查询条件
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "操作日志查询条件")
public class OperationLogQueryDTO {

    @Schema(description = "操作人用户名，模糊匹配")
    private String operator;

    @Schema(description = "操作类型（操作描述），模糊匹配，如「审核」「核销」「强制取消」")
    private String operation;

    @Schema(description = "角色筛选 1用户 2匠人 3管理员")
    private Integer role;

    @Schema(description = "是否成功 0否 1是；不传表示全部")
    private Integer success;

    @Schema(description = "开始时间（含），格式 yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间（含），格式 yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，默认 20")
    private Long pageSize = 20L;

    /** 归一化：分页防越界、空串转 null（空串参与 LIKE 会变成 `%%`，把筛选条件悄悄吃掉） */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20L;
        }
        if (pageSize > 200) {
            pageSize = 200L;
        }
        operator = blankToNull(operator);
        operation = blankToNull(operation);
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
