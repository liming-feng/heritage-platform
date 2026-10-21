package com.lmf.heritage.module.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（由 {@code OperationLogAspect} 异步入库）
 *
 * <p><b>为什么不继承 BaseEntity / 不加 deleted 字段</b>：日志是只增不改的事实记录，
 * 逻辑删除会让「谁在什么时候改了什么」失去可追溯性；同时全局配置
 * {@code logic-delete-field: deleted} 只对<b>含有该字段的实体</b>生效，
 * 本实体没有该字段，因此查询不会被自动拼上 {@code deleted = 0}。</p>
 *
 * <p><b>操作人是快照而不是外键</b>：{@code operator} / {@code role} 存的是操作发生那一刻的
 * 用户名与角色。用户改名、角色变更、账号注销之后，日志里仍要能读出「当时是谁做的」——
 * 如果只存 {@code operator_id} 再去 JOIN，历史事实就会跟着当前状态漂移。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("operation_log")
@Schema(description = "操作日志")
public class OperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "操作人 user.id（未登录为空）")
    private Long operatorId;

    @Schema(description = "操作人用户名（快照）")
    private String operator;

    @Schema(description = "角色 1用户 2匠人 3管理员")
    private Integer role;

    @Schema(description = "操作描述，取自 @OperationLog 注解")
    private String operation;

    @Schema(description = "方法签名 类名.方法名")
    private String method;

    @Schema(description = "请求参数（已脱敏与截断）")
    private String params;

    @Schema(description = "操作 IP")
    private String ip;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "是否成功 0否 1是")
    private Integer success;

    @Schema(description = "异常信息（成功时为空）")
    private String errorMsg;

    /**
     * 操作时间
     *
     * <p>由数据库 {@code DEFAULT CURRENT_TIMESTAMP} 落库，不走 MyBatis-Plus 自动填充。
     * 日志写入发生在异步线程里，用 Java 侧时间会产生「日志时间早于/晚于实际请求」
     * 的微小漂移；交给数据库统一取值，多条日志的相对顺序才稳定。</p>
     */
    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
