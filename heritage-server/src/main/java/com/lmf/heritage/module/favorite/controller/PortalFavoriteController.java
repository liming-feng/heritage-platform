package com.lmf.heritage.module.favorite.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.favorite.dto.FavoriteDTO;
import com.lmf.heritage.module.favorite.dto.FavoriteQueryDTO;
import com.lmf.heritage.module.favorite.service.FavoriteService;
import com.lmf.heritage.module.favorite.vo.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端 · 收藏
 *
 * <p>商品、课程、资讯共用一套接口，靠 {@code targetType} 区分。
 * 收藏与取消<b>都是幂等</b>的：重复点不会报错，也不会多出记录 ——
 * 移动端网络抖动时用户往往会连点两下，接口不该因此给一个红色报错。</p>
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-收藏", description = "收藏/取消/状态查询/我的收藏（商品·课程·资讯共用）")
@RestController
@RequestMapping("/portal/favorite")
@RequiredArgsConstructor
@LoginRequired
public class PortalFavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏", description = "targetType 1商品 2课程 3资讯；重复收藏不会报错也不会重复记录")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Valid FavoriteDTO dto) {
        favoriteService.add(dto);
        return Result.success();
    }

    @Operation(summary = "取消收藏", description = "幂等：本来就没收藏时也返回成功")
    @PostMapping("/remove")
    public Result<Void> remove(@RequestBody @Valid FavoriteDTO dto) {
        favoriteService.remove(dto);
        return Result.success();
    }

    @Operation(summary = "是否已收藏", description = "详情页收藏按钮的初始状态")
    @GetMapping("/status")
    public Result<Boolean> status(@RequestParam("targetType") Integer targetType,
                                  @RequestParam("targetId") Long targetId) {
        return Result.success(favoriteService.isFavorited(targetType, targetId,
                com.lmf.heritage.security.UserContext.userIdOrNull()));
    }

    @Operation(summary = "我的收藏", description = "targetType 不传表示全部；失效目标会带 targetAvailable=false 供前端置灰")
    @GetMapping("/mine")
    public Result<PageResult<FavoriteVO>> mine(FavoriteQueryDTO query) {
        return Result.success(favoriteService.mine(query));
    }

    @Operation(summary = "我的收藏数量统计", description = "返回 {1:商品数, 2:课程数, 3:资讯数}，用于 tab 角标")
    @GetMapping("/mine/count")
    public Result<Map<Integer, Integer>> myCount() {
        return Result.success(favoriteService.myCount());
    }
}
