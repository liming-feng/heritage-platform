package com.lmf.heritage.module.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志展示行
 *
 * <p>比实体多三个「给人看」的派生字段（{@code roleText} / {@code successText} / {@code costText}）。
 * 让前端自己去翻译枚举，等于把同一套文案规则散落到前端每个用到日志的页面；
 * 放后端只翻译一次。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "操作日志展示行")
public class OperationLogVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "操作人 user.id")
    private Long operatorId;

    @Schema(description = "操作人用户名")
    private String operator;

    @Schema(description = "角色 1用户 2匠人 3管理员")
    private Integer role;

    @Schema(description = "角色文案")
    private String roleText;

    @Schema(description = "操作描述")
    private String operation;

    @Schema(description = "方法签名")
    private String method;

    @Schema(description = "请求参数")
    private String params;

    @Schema(description = "操作 IP")
    private String ip;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "耗时文案，如「35 ms」")
    private String costText;

    @Schema(description = "是否成功 0否 1是")
    private Integer success;

    @Schema(description = "结果文案「成功 / 失败」")
    private String successText;

    @Schema(description = "异常信息")
    private String errorMsg;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
