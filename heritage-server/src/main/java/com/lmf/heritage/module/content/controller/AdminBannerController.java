package com.lmf.heritage.module.content.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.content.dto.AdminBannerQueryDTO;
import com.lmf.heritage.module.content.dto.BannerBatchSortDTO;
import com.lmf.heritage.module.content.dto.BannerSaveDTO;
import com.lmf.heritage.module.content.service.AdminBannerService;
import com.lmf.heritage.module.content.vo.AdminBannerVO;
import com.lmf.heritage.module.content.vo.BannerExcelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 管理端 · 轮播图管理
 *
 * <p>所有写操作都会清 {@code banner:pos:*} 缓存：轮播是首页第一屏，
 * 换图后要等 30 分钟缓存过期才生效，是不可接受的。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-轮播管理", description = "轮播图增删改查 / 启停 / 排序 / 导出")
@RestController
@RequestMapping("/admin/banner")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    @Operation(summary = "轮播图列表",
            description = "含停用中的图（只查启用项会让「停用」变成单向操作）。"
                    + "可按 position（1首页 2课程页）与 status 筛选，按 position、sort 升序排列")
    @GetMapping("/list")
    public Result<PageResult<AdminBannerVO>> list(AdminBannerQueryDTO query) {
        return Result.success(adminBannerService.page(query));
    }

    @Operation(summary = "轮播图详情")
    @GetMapping("/{id}")
    public Result<AdminBannerVO> detail(@PathVariable("id") Long id) {
        return Result.success(adminBannerService.detail(id));
    }

    @Operation(summary = "新增 / 编辑轮播图",
            description = "image 必填。不传 id 为新增：position 默认首页、sort 排到该位置末尾、status 默认启用；"
                    + "传 id 为编辑，未传的字段保持原值。编辑时若改了 position 且未传 sort，"
                    + "排序号会重新落到新位置末尾")
    @OperationLog("轮播图保存")
    @PostMapping("/save")
    public Result<Long> save(@RequestBody @Valid BannerSaveDTO dto) {
        return Result.success(adminBannerService.save(dto));
    }

    @Operation(summary = "启用 / 停用轮播图",
            description = "status 1启用 0停用。停用后前台该位置的轮播立即不再返回这张图")
    @OperationLog("轮播图启停")
    @PostMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") Integer status) {
        adminBannerService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "批量调整排序与启停",
            description = "单次最多 100 张。每项 sort / status 至少传一个，都不传的项会被跳过；"
                    + "返回实际生效条数")
    @OperationLog("轮播图批量排序")
    @PostMapping("/batch-sort")
    public Result<Integer> batchSort(@RequestBody @Valid BannerBatchSortDTO dto) {
        return Result.success(adminBannerService.batchSort(dto));
    }

    @Operation(summary = "删除轮播图", description = "逻辑删除")
    @OperationLog("轮播图删除")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        adminBannerService.remove(id);
        return Result.success();
    }

    @Operation(summary = "导出轮播图 Excel",
            description = "筛选条件与列表一致，最多 1000 条")
    @GetMapping("/export")
    public void export(AdminBannerQueryDTO query, HttpServletResponse response) throws IOException {
        List<BannerExcelVO> rows = adminBannerService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("轮播图列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), BannerExcelVO.class)
                .sheet("轮播图列表")
                .doWrite(rows);
        log.info("[轮播管理] 导出 {} 条", rows.size());
    }
}
