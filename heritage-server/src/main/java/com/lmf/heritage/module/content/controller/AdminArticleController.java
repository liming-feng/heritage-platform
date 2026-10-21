package com.lmf.heritage.module.content.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.content.dto.AdminArticleQueryDTO;
import com.lmf.heritage.module.content.dto.ArticleBatchStatusDTO;
import com.lmf.heritage.module.content.dto.ArticleSaveDTO;
import com.lmf.heritage.module.content.service.AdminArticleService;
import com.lmf.heritage.module.content.vo.AdminArticleDetailVO;
import com.lmf.heritage.module.content.vo.AdminArticleVO;
import com.lmf.heritage.module.content.vo.ArticleExcelVO;
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
 * 管理端 · 资讯内容管理
 *
 * <p>发布 / 下架 / 置顶 / 删除都打了 {@code @OperationLog}：这些操作直接改变
 * 全站用户看到的内容，「谁把首页头条换掉了」是必须有据可查的。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-资讯管理", description = "资讯增删改查 / 发布下架 / 置顶 / 批量操作 / 导出")
@RestController
@RequestMapping("/admin/article")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminArticleController {

    private final AdminArticleService adminArticleService;

    @Operation(summary = "资讯列表",
            description = "含草稿与已下架（用户端列表看不到这两类）。"
                    + "支持 keyword（标题/作者/摘要）、categoryId、status、isTop、发布时间范围筛选；"
                    + "sort new最新(默认)/view/like/comment；置顶项永远排在最前")
    @GetMapping("/list")
    public Result<PageResult<AdminArticleVO>> list(AdminArticleQueryDTO query) {
        return Result.success(adminArticleService.page(query));
    }

    @Operation(summary = "资讯详情", description = "含富文本正文，用于编辑页回显")
    @GetMapping("/{id}")
    public Result<AdminArticleDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(adminArticleService.detail(id));
    }

    @Operation(summary = "新增 / 编辑资讯",
            description = "不传 id 为新增（默认存草稿 status=0，不置顶）；传 id 为编辑，"
                    + "未传的 status / isTop 保持原值。categoryId 不存在时返回 3008")
    @OperationLog("资讯保存")
    @PostMapping("/save")
    public Result<Long> save(@RequestBody @Valid ArticleSaveDTO dto) {
        return Result.success(adminArticleService.save(dto));
    }

    @Operation(summary = "发布 / 下架单篇",
            description = "status 1发布 0下架（回到草稿态）。下架后用户端立即查不到该资讯，缓存同步清除")
    @OperationLog("资讯状态变更")
    @PostMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") Integer status) {
        adminArticleService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "批量发布 / 下架",
            description = "单次最多 100 篇，返回实际生效条数。幂等：对已经处于目标状态的资讯可重复提交")
    @OperationLog("资讯批量状态变更")
    @PostMapping("/batch-status")
    public Result<Integer> batchStatus(@RequestBody @Valid ArticleBatchStatusDTO dto) {
        return Result.success(adminArticleService.batchStatus(dto));
    }

    @Operation(summary = "置顶 / 取消置顶",
            description = "isTop 1置顶 0取消。置顶项在用户端列表与资讯管理中都排在最前")
    @OperationLog("资讯置顶")
    @PostMapping("/{id}/top")
    public Result<Void> updateTop(@PathVariable("id") Long id,
                                 @RequestParam("isTop") Integer isTop) {
        adminArticleService.updateTop(id, isTop);
        return Result.success();
    }

    @Operation(summary = "删除资讯",
            description = "逻辑删除。评论与点赞仍挂在 article_id 上，物理删会让这些记录变成孤儿数据")
    @OperationLog("资讯删除")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        adminArticleService.remove(id);
        return Result.success();
    }

    @Operation(summary = "导出资讯 Excel",
            description = "筛选条件与列表一致，最多 5000 条。浏览量/点赞/评论导出为数值型，可直接排序")
    @GetMapping("/export")
    public void export(AdminArticleQueryDTO query, HttpServletResponse response) throws IOException {
        List<ArticleExcelVO> rows = adminArticleService.exportList(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("资讯列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ArticleExcelVO.class)
                .sheet("资讯列表")
                .doWrite(rows);
        log.info("[资讯管理] 导出 {} 条", rows.size());
    }
}
