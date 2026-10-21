package com.lmf.heritage.module.user.controller;

import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.RateLimit;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.user.dto.LoginDTO;
import com.lmf.heritage.module.user.dto.RefreshTokenDTO;
import com.lmf.heritage.module.user.dto.RegisterDTO;
import com.lmf.heritage.module.user.service.UserService;
import com.lmf.heritage.module.user.vo.LoginVO;
import com.lmf.heritage.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 · 认证接口
 *
 * <p>路径不含 /api：context-path 已经补上，避免出现 /api/api/auth/login。</p>
 */
@Tag(name = "用户端-认证", description = "注册 / 登录 / 刷新令牌 / 退出登录")
@RestController
@RequestMapping("/portal/auth")
@RequiredArgsConstructor
public class PortalAuthController {

    private final UserService userService;

    @Value("${heritage.jwt.header:Authorization}")
    private String tokenHeader;

    @Value("${heritage.jwt.prefix:Bearer }")
    private String tokenPrefix;

    @Operation(summary = "注册", description = "注册成功直接返回双 Token 与用户信息")
    @RateLimit(count = 5, window = 60, message = "注册过于频繁，请稍后再试")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success("注册成功", userService.register(dto));
    }

    @Operation(summary = "登录", description = "返回 access_token(2h) + refresh_token(7d)")
    @RateLimit(count = 10, window = 60, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", userService.login(dto));
    }

    @Operation(summary = "刷新令牌", description = "用 refresh_token 换发新的双 Token")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(userService.refresh(dto.getRefreshToken()));
    }

    @Operation(summary = "退出登录", description = "把当前 token 写入 Redis 黑名单，立即失效")
    @LoginRequired
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                               @RequestBody(required = false) RefreshTokenDTO dto) {
        userService.logout(JwtUtil.stripPrefix(authHeader, tokenPrefix),
                dto == null ? null : dto.getRefreshToken());
        return Result.success("已退出登录", null);
    }
}
