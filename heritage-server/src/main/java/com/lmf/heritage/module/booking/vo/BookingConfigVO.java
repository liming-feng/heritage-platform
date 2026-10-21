package com.lmf.heritage.module.booking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预约规则配置视图（管理端）
 *
 * <p>除键值外还带回 {@code unit} 与 {@code valueType}，让前端能渲染出
 * 「输入框 + 单位」而不是一堆看不懂的字符串。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "预约规则配置")
public class BookingConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置 id")
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "值的语义类型：int / boolean")
    private String valueType;

    @Schema(description = "单位，如 小时 / 次")
    private String unit;

    @Schema(description = "最近修改时间")
    private java.time.LocalDateTime updateTime;
}
