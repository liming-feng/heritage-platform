package com.lmf.heritage.module.user.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.user.dto.ChangePasswordDTO;
import com.lmf.heritage.module.user.dto.UpdateProfileDTO;
import com.lmf.heritage.module.user.service.UserService;
import com.lmf.heritage.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 · 个人中心接口
 */
@Tag(name = "用户端-个人中心", description = "当前登录用户信息、资料修改、密码修改")
@RestController
@RequestMapping("/portal/user")
@RequiredArgsConstructor
public class PortalUserController {

    private final UserService userService;

    @Operation(summary = "当前登录用户信息")
    @LoginRequired
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.success(userService.currentUser());
    }

    @Operation(summary = "修改个人资料", description = "只能改自己的昵称/头像/手机号/邮箱/性别")
    @LoginRequired
    @PutMapping("/info")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success("资料已更新", userService.updateProfile(dto));
    }

    @Operation(summary = "修改密码")
    @LoginRequired
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success("密码修改成功，请重新登录", null);
    }
}
