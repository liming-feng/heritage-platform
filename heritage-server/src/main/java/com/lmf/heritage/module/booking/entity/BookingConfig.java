package com.lmf.heritage.module.booking.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约规则配置项（键值对形式，管理端可改）
 *
 * <p>把规则做成数据而不是常量，是为了让「预约行为随配置立刻改变」这件事可验证：
 * 管理端改一条记录 → 清 Redis 缓存 → 下一次提交 / 取消就走新规则，无需重启服务。</p>
 *
 * <p>本表有意不做逻辑删除（配置项要能被管理端删除重建），因此没有 {@code deleted} 字段。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@TableName("booking_config")
@Schema(description = "预约规则配置")
public class BookingConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "配置键，如 audit_required")
    private String configKey;

    @Schema(description = "配置值（统一以字符串存储，读取时按语义转换）")
    private String configValue;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "说明")
    private String description;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
