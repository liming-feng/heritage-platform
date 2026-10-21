package com.lmf.heritage.security;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文（基于 ThreadLocal，由 JwtInterceptor 写入 / 清理）
 */
public class UserContext {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUser {
        private Long userId;
        private String username;
        private Integer role;
    }

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 取当前登录用户，未登录返回 null */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 取当前登录用户，未登录抛异常 */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return user;
    }

    public static Long userId() {
        return require().getUserId();
    }

    /** 未登录时返回 null，用于「登录可选」的接口（浏览历史、浏览量等） */
    public static Long userIdOrNull() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static Integer role() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static boolean isAdmin() {
        return Integer.valueOf(Constants.ROLE_ADMIN).equals(role());
    }

    public static boolean isCraftsman() {
        return Integer.valueOf(Constants.ROLE_CRAFTSMAN).equals(role());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
