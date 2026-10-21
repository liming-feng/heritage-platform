package com.lmf.heritage.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录 / 注册 / 刷新返回：双 Token + 用户信息
 */
@Data
@Schema(description = "登录结果（双 Token + 用户信息）")
public class LoginVO {

    @Schema(description = "访问令牌，放入请求头 Authorization: Bearer {accessToken}")
    private String accessToken;

    @Schema(description = "刷新令牌，accessToken 过期后用它换新的")
    private String refreshToken;

    @Schema(description = "令牌类型，固定 Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "accessToken 有效期（秒）")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserVO user;

    public static LoginVO of(String accessToken, String refreshToken, long expiresIn, UserVO user) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(expiresIn);
        vo.setUser(user);
        return vo;
    }
}
