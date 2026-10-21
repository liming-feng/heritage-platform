package com.lmf.heritage.module.user.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.user.dto.AdminUserQueryDTO;
import com.lmf.heritage.module.user.dto.UserBatchStatusDTO;
import com.lmf.heritage.module.user.dto.UserStatusDTO;
import com.lmf.heritage.module.user.service.AdminUserService;
import com.lmf.heritage.module.user.vo.AdminUserVO;
import com.lmf.heritage.module.user.vo.RegisterStatVO;
import com.lmf.heritage.module.user.vo.UserExcelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端 · 用户管理
 *
 * <p>三个动作（禁用/启用、批量禁用/启用、导出）都打了 {@code @OperationLog}：
 * 账号启停是能直接改变他人使用权的操作，出了问题必须能查出「谁在什么时候禁了谁」。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-用户管理", description = "用户/匠人列表、启停、注册量统计、导出")
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户列表",
            description = "role 1用户/2匠人/3管理员，status 0禁用/1正常，auditStatus 匠人资质状态，"
                    + "keyword 匹配用户名/昵称/手机号/匠人真实姓名，startTime~endTime 注册时间范围；"
                    + "匠人账号会一并带出资质字段")
    @GetMapping("/list")
    public Result<PageResult<AdminUserVO>> list(AdminUserQueryDTO query) {
        return Result.success(adminUserService.page(query));
    }

    @Operation(summary = "用户详情", description = "匠人账号额外返回资质信息（真实姓名、门类、审核状态、驳回原因）")
    @GetMapping("/{id}")
    public Result<AdminUserVO> detail(@PathVariable("id") Long id) {
        return Result.success(adminUserService.detail(id));
    }

    @Operation(summary = "启用 / 禁用账号",
            description = "status 0 禁用 / 1 正常。禁用立即生效（状态缓存同步刷新，已签发的令牌下一个请求即被拒）；"
                    + "管理员账号不允许禁用 —— 平台没有超级管理员角色，禁掉就没人能解回来了")
    @OperationLog("用户启停")
    @PostMapping("/status")
    public Result<Void> updateStatus(@RequestBody @Valid UserStatusDTO dto) {
        adminUserService.updateStatus(dto);
        return Result.success();
    }

    @Operation(summary = "批量启用 / 禁用账号",
            description = "单次最多 50 个。批内含管理员账号时整批拒绝（不跳过、不部分成功），"
                    + "返回实际发生变化的条数（已是目标状态的会被跳过）")
    @OperationLog("用户批量启停")
    @PostMapping("/batch/status")
    public Result<Integer> batchUpdateStatus(@RequestBody @Valid UserBatchStatusDTO dto) {
        return Result.success(adminUserService.batchUpdateStatus(dto));
    }

    @Operation(summary = "注册量统计",
            description = "累计/今日/近7天/本月新增 + 匠人总量与待审数 + 近 N 天日趋势（连续补零）+ "
                    + "近 N 月月趋势（连续补零）+ 角色分布（三种角色都返回，没有的为 0）。"
                    + "days 默认 30 上限 90，months 默认 6 上限 24")
    @GetMapping("/stat/register")
    public Result<RegisterStatVO> registerStat(
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam(value = "months", required = false) Integer months) {
        return Result.success(adminUserService.registerStat(days, months));
    }

    @Operation(summary = "导出用户 Excel",
            description = "筛选条件与列表一致，最多 5000 条。出于隐私考虑不导出邮箱与密码")
    @GetMapping("/export")
    public void export(AdminUserQueryDTO query, HttpServletResponse response) throws IOException {
        List<UserExcelVO> rows = adminUserService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("用户列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), UserExcelVO.class)
                .sheet("用户列表")
                .doWrite(rows);
        log.info("[用户管理] 导出 {} 条", rows.size());
    }
}
