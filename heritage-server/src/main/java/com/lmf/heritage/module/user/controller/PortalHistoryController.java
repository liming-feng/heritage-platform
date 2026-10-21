package com.lmf.heritage.module.user.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.user.dto.HistoryUploadDTO;
import com.lmf.heritage.module.user.service.HistoryService;
import com.lmf.heritage.module.user.vo.HistoryGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 · 浏览历史
 *
 * <p>未登录时记录存 localStorage，登录后由前端一次性上报，这里负责去重落库与按日期分组返回。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-浏览历史", description = "上报 / 列表（按日期分组）/ 删除 / 清空")
@RestController
@RequestMapping("/portal/history")
@RequiredArgsConstructor
public class PortalHistoryController {

    private final HistoryService historyService;

    @Operation(summary = "上报浏览历史",
            description = "批量上报，单次最多 50 条；按 (user, type, target) 去重，重复的只刷新浏览时间")
    @LoginRequired
    @PostMapping("/upload")
    public Result<Integer> upload(@Valid @RequestBody HistoryUploadDTO dto) {
        return Result.success("上报成功", historyService.upload(dto));
    }

    @Operation(summary = "浏览历史列表", description = "按日期分组返回（今天 / 昨天 / 具体日期）")
    @LoginRequired
    @GetMapping("/list")
    public Result<List<HistoryGroupVO>> list(
            @Parameter(description = "可选，1商品 2课程 3资讯")
            @RequestParam(value = "targetType", required = false) Integer targetType) {
        return Result.success(historyService.listGrouped(targetType));
    }

    @Operation(summary = "删除一条浏览历史")
    @LoginRequired
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam("targetType") Integer targetType,
                               @RequestParam("targetId") Long targetId) {
        historyService.delete(targetType, targetId);
        return Result.success("已删除", null);
    }

    @Operation(summary = "清空浏览历史")
    @LoginRequired
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        historyService.clear();
        return Result.success("已清空", null);
    }
}
