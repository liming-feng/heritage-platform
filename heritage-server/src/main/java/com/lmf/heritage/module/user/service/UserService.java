package com.lmf.heritage.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.module.user.dto.ChangePasswordDTO;
import com.lmf.heritage.module.user.dto.LoginDTO;
import com.lmf.heritage.module.user.dto.RegisterDTO;
import com.lmf.heritage.module.user.dto.UpdateProfileDTO;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.vo.LoginVO;
import com.lmf.heritage.module.user.vo.UserVO;

/**
 * 用户服务：注册 / 登录 / 刷新令牌 / 退出 / 个人资料
 */
public interface UserService extends IService<User> {

    /** 注册（成功后直接返回双 Token，前端无需再调一次登录） */
    LoginVO register(RegisterDTO dto);

    /** 登录 */
    LoginVO login(LoginDTO dto);

    /** 用 refresh_token 换发新的双 Token */
    LoginVO refresh(String refreshToken);

    /** 退出登录：把 access_token（可附带 refresh_token）写入 Redis 黑名单 */
    void logout(String accessToken, String refreshToken);

    /** 当前登录用户信息 */
    UserVO currentUser();

    /** 修改个人资料 */
    UserVO updateProfile(UpdateProfileDTO dto);

    /** 修改密码 */
    void changePassword(ChangePasswordDTO dto);
}
