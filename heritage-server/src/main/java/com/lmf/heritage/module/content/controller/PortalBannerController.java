package com.lmf.heritage.module.content.controller;

import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.content.service.BannerService;
import com.lmf.heritage.module.content.vo.BannerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 · 轮播图
 *
 * @author 李明峰 23100025
 */
@Tag(name = "用户端-轮播图", description = "按位置读取启用中的轮播图")
@RestController
@RequestMapping("/portal/banner")
@RequiredArgsConstructor
public class PortalBannerController {

    private final BannerService bannerService;

    @Operation(summary = "轮播图列表",
            description = "position 位置（1首页 2课程页，默认 1）；只返回启用中的图，按 sort 升序，最多 10 张")
    @GetMapping("/list")
    public Result<List<BannerVO>> list(@RequestParam(value = "position", defaultValue = "1") Integer position,
                                       @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return Result.success(bannerService.listByPosition(position, limit));
    }
}
