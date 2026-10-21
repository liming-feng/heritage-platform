package com.lmf.heritage.module.craftsman.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.craftsman.dto.CraftsmanQueryDTO;
import com.lmf.heritage.module.craftsman.service.CraftsmanService;
import com.lmf.heritage.module.craftsman.vo.CraftsmanDetailVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 · 匠人
 *
 * <p>列表与主页允许游客浏览，关注相关的操作需要登录。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-匠人", description = "匠人列表 / 主页 / 关注")
@RestController
@RequestMapping("/portal/craftsman")
@RequiredArgsConstructor
public class PortalCraftsmanController {

    private final CraftsmanService craftsmanService;

    @Operation(summary = "匠人列表",
            description = "支持关键词 keyword、技艺门类 craftType、排序 sort（default/follower/goods/new）、分页")
    @GetMapping("/list")
    public Result<PageResult<CraftsmanSimpleVO>> list(CraftsmanQueryDTO query) {
        return Result.success(craftsmanService.pageList(query));
    }

    @Operation(summary = "匠人主页",
            description = "履历 / 师承 / 代表作与在售商品 / 开设的课程 / 是否已关注（未登录时 followed 恒为 false）")
    @GetMapping("/{id}")
    public Result<CraftsmanDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(craftsmanService.detail(id));
    }

    @Operation(summary = "关注匠人", description = "重复关注幂等，不会报错")
    @LoginRequired
    @PostMapping("/follow/{id}")
    public Result<Void> follow(@PathVariable("id") Long id) {
        craftsmanService.follow(id);
        return Result.success();
    }

    @Operation(summary = "取消关注匠人", description = "未关注时调用也返回成功（幂等）")
    @LoginRequired
    @DeleteMapping("/follow/{id}")
    public Result<Void> unfollow(@PathVariable("id") Long id) {
        craftsmanService.unfollow(id);
        return Result.success();
    }

    @Operation(summary = "我的关注列表", description = "按关注时间倒序，只返回仍通过资质审核的匠人")
    @LoginRequired
    @GetMapping("/follow/list")
    public Result<List<CraftsmanSimpleVO>> myFollows() {
        return Result.success(craftsmanService.myFollows());
    }
}
