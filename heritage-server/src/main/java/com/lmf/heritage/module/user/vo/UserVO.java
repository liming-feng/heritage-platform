package com.lmf.heritage.module.user.vo;

import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象（绝不包含密码）
 */
@Data
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "角色 1普通用户 2匠人 3管理员")
    private Integer role;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "状态 0禁用 1正常")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;

    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setGender(user.getGender());
        vo.setRole(user.getRole());
        vo.setRoleName(roleName(user.getRole()));
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private static String roleName(Integer role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case Constants.ROLE_USER -> "普通用户";
            case Constants.ROLE_CRAFTSMAN -> "匠人";
            case Constants.ROLE_ADMIN -> "管理员";
            default -> "未知";
        };
    }
}
