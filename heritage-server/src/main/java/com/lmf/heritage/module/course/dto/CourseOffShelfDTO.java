package com.lmf.heritage.module.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理端 · 课程违规下架
 *
 * <p>与商品同理：只把课程状态改掉是不够的，必须让平台的处置结论钉住，
 * 否则匠人在课程管理里重新开放预约就把它绕过去了。因此这里置
 * {@code audit_status=2} 并把原因写成「【违规下架】…」，前台立即不可见。</p>
 *
 * <p><b>已经产生预约的课程被下架时会怎样</b>：预约记录本身不动 ——
 * 名额占用、核销码、站内消息都还在，匠人仍能在预约管理里完成核销。
 * 下架只影响「前台还发不发这门课」，不回溯取消已成立的预约
 * （那需要给每位用户退钱/发通知，属于「强制取消预约」这个独立动作的事，
 * 见 {@code POST /admin/booking/cancel}）。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "课程违规下架参数")
public class CourseOffShelfDTO {

    @NotEmpty(message = "请至少选择一门课程")
    @Size(max = 50, message = "单次最多下架 50 门")
    @Schema(description = "课程 id 列表，单次最多 50 个", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @NotBlank(message = "请填写下架原因")
    @Size(max = 200, message = "下架原因不能超过 200 字")
    @Schema(description = "违规下架原因（必填，会展示给匠人）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
