package com.lmf.heritage.module.goods.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.goods.dto.AdminGoodsQueryDTO;
import com.lmf.heritage.module.goods.dto.GoodsAuditDTO;
import com.lmf.heritage.module.goods.dto.GoodsBatchAuditDTO;
import com.lmf.heritage.module.goods.dto.GoodsOffShelfDTO;
import com.lmf.heritage.module.goods.service.AdminGoodsService;
import com.lmf.heritage.module.goods.vo.AdminGoodsDetailVO;
import com.lmf.heritage.module.goods.vo.AdminGoodsVO;
import com.lmf.heritage.module.goods.vo.GoodsExcelVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端 · 商品审核
 *
 * <p>审核、批量审核、违规下架三个动作都打了 {@code @OperationLog}：
 * 平台对一件商品的放行或封禁决定直接关系到匠人的经营，
 * 「谁审的、什么时候、给了什么理由」必须可追溯。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-商品审核", description = "全平台商品列表 / 审核详情 / 通过驳回 / 批量审核 / 违规下架 / 导出")
@RestController
@RequestMapping("/admin/goods")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminGoodsController {

    private final AdminGoodsService adminGoodsService;

    @Operation(summary = "全平台商品列表",
            description = "status 0下架/1在售/2待审核，auditStatus 0待审核/1已通过/2已驳回，"
                    + "categoryId 分类，craftsmanId 按匠人下钻，keyword 匹配商品名/副标题/匠人姓名，"
                    + "minPrice~maxPrice 价格区间，startTime~endTime 发布时间范围；"
                    + "sort new(默认)/sales/stock/price/view")
    @GetMapping("/list")
    public Result<PageResult<AdminGoodsVO>> list(AdminGoodsQueryDTO query) {
        return Result.success(adminGoodsService.page(query));
    }

    @Operation(summary = "商品审核详情",
            description = "含全部图片与规格、匠人联系方式、历史驳回原因")
    @GetMapping("/{id}/audit-detail")
    public Result<AdminGoodsDetailVO> auditDetail(@PathVariable("id") Long id) {
        return Result.success(adminGoodsService.auditDetail(id));
    }

    @Operation(summary = "商品审核（通过并上架 / 驳回）",
            description = "仅「待审核」的商品可审，否则返回 8003。通过时同时置为上架（status=1）并清空历史驳回原因；"
                    + "驳回时 reason 必填（8006），商品回到下架状态")
    @OperationLog("商品审核")
    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody @Valid GoodsAuditDTO dto) {
        adminGoodsService.audit(dto);
        return Result.success();
    }

    @Operation(summary = "批量审核",
            description = "单次最多 50 件，<b>整批回滚</b>：批内只要有一件不是待审核状态，整批拒绝并返回 8003，"
                    + "不做部分成功。通过时全部上架")
    @OperationLog("商品批量审核")
    @PostMapping("/audit/batch")
    public Result<Integer> batchAudit(@RequestBody @Valid GoodsBatchAuditDTO dto) {
        return Result.success(adminGoodsService.batchAudit(dto));
    }

    @Operation(summary = "违规下架",
            description = "单次最多 50 件，reason 必填。起始状态必须是「已通过」，否则返回 8003。"
                    + "下架会把审核状态一并置为「已驳回」并把原因写成「【违规下架】原因」，"
                    + "因此匠人无法自行重新上架，必须先改稿重新提交审核")
    @OperationLog("商品违规下架")
    @PostMapping("/off-shelf")
    public Result<Integer> offShelf(@RequestBody @Valid GoodsOffShelfDTO dto) {
        return Result.success(adminGoodsService.offShelf(dto));
    }

    @Operation(summary = "导出商品 Excel",
            description = "筛选条件与列表一致，最多 5000 条。价格与库存导出为数值型，可直接排序求和")
    @GetMapping("/export")
    public void export(AdminGoodsQueryDTO query, HttpServletResponse response) throws IOException {
        List<GoodsExcelVO> rows = adminGoodsService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("商品列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), GoodsExcelVO.class)
                .sheet("商品列表")
                .doWrite(rows);
        log.info("[商品审核] 导出 {} 条", rows.size());
    }
}
