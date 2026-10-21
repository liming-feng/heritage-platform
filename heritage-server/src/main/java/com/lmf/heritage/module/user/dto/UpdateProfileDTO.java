package com.lmf.heritage.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求
 *
 * <p>用户名与密码不在此处修改：用户名是登录标识，密码走单独的修改密码接口。</p>
 */
@Data
@Schema(description = "修改个人资料请求")
public class UpdateProfileDTO {

    @Size(max = 50, message = "昵称最长 50 个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;
}
