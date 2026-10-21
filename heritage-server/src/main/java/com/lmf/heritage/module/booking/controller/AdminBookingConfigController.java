package com.lmf.heritage.module.booking.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.booking.dto.BookingConfigUpdateDTO;
import com.lmf.heritage.module.booking.service.BookingConfigService;
import com.lmf.heritage.module.booking.vo.BookingConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 · 预约规则配置（<b>配置驱动</b>）
 *
 * <p>本阶段先出接口，管理页面在 P8 实现。改完配置会立即清 Redis 缓存，
 * 下一次提交 / 取消预约即按新规则执行，无需重启服务。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "管理端-预约规则", description = "预约规则配置的查询与修改")
@RestController
@RequestMapping("/admin/booking-config")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminBookingConfigController {

    private final BookingConfigService configService;

    @Operation(summary = "配置列表",
            description = "返回 5 项规则：最少提前预约时长、开课前可取消时限、每人每课程限约次数、"
                    + "是否强制审核、超时未审自动拒绝时长；并带回 valueType 与 unit 便于前端渲染表单")
    @GetMapping("/list")
    public Result<List<BookingConfigVO>> list() {
        return Result.success(configService.listAll());
    }

    @Operation(summary = "修改配置",
            description = "按键的语义校验取值：audit_required 只能 0/1；各时长 0~720 小时；限约次数 0~10。"
                    + "修改后立即清缓存，后续预约行为随之改变")
    @OperationLog("修改预约规则")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody @Valid BookingConfigUpdateDTO dto) {
        configService.update(dto);
        return Result.success();
    }

    @Operation(summary = "恢复默认配置", description = "一键还原为出厂规则，返回实际变更的项数（演示与答辩时用）")
    @PostMapping("/reset")
    public Result<Integer> reset() {
        return Result.success(configService.resetToDefault());
    }
}
