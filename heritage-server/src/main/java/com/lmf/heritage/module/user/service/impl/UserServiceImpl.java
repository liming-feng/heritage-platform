package com.lmf.heritage.module.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.user.dto.ChangePasswordDTO;
import com.lmf.heritage.module.user.dto.LoginDTO;
import com.lmf.heritage.module.user.dto.RegisterDTO;
import com.lmf.heritage.module.user.dto.UpdateProfileDTO;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.mapper.UserMapper;
import com.lmf.heritage.module.user.service.UserService;
import com.lmf.heritage.module.user.vo.LoginVO;
import com.lmf.heritage.module.user.vo.UserVO;
import com.lmf.heritage.security.JwtUtil;
import com.lmf.heritage.security.UserContext;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户服务实现
 *
 * <p>JWT 双 Token 策略见规划书 7.1：
 * access_token 2 小时用于业务鉴权；refresh_token 7 天仅用于换发。
 * 退出登录时把 token 写入 Redis 黑名单，TTL 取该 token 的剩余有效期，
 * 因此黑名单不会无限膨胀。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** 新用户默认头像（占位图，离线可用） */
    private static final String DEFAULT_AVATAR = "/api/uploads/demo/avatar-default.svg";
    /** 新用户默认昵称前缀 */
    private static final String DEFAULT_NICKNAME_PREFIX = "非遗用户";

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto) {
        // 1. 用户名唯一性校验（数据库还有 uk_username 兜底，这里是为了给出友好提示）
        long exists = count(Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        BusinessException.throwIf(exists > 0, ErrorCode.USERNAME_EXISTS);

        // 2. 落库：密码 BCrypt 加密；注册的账号一律是普通用户，匠人/管理员由后台或初始化数据产生
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? DEFAULT_NICKNAME_PREFIX + ThreadLocalRandom.current().nextInt(1000, 9999)
                : dto.getNickname().trim());
        user.setAvatar(DEFAULT_AVATAR);
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(0);
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.STATUS_ENABLED);
        user.setDeleted(0);
        save(user);

        log.info("新用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));

        // 用户不存在与密码错误统一提示，避免被枚举用户名
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        BusinessException.throwIf(!Integer.valueOf(Constants.STATUS_ENABLED).equals(user.getStatus()),
                ErrorCode.USER_DISABLED);

        log.info("用户登录成功: id={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return buildLoginVO(user);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (BusinessException e) {
            // refresh_token 过期或非法：让前端跳回登录页
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        if (!JwtUtil.TYPE_REFRESH.equals(jwtUtil.getType(claims))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 已退出的 refresh_token 不能再用
        if (Boolean.TRUE.equals(redisTemplate.hasKey(Constants.KEY_TOKEN_BLACKLIST + refreshToken))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        Long userId = jwtUtil.getUserId(claims);
        User user = getById(userId);
        BusinessException.throwIf(user == null, ErrorCode.USER_NOT_FOUND);
        BusinessException.throwIf(!Integer.valueOf(Constants.STATUS_ENABLED).equals(user.getStatus()),
                ErrorCode.USER_DISABLED);

        log.info("刷新令牌成功: userId={}", userId);
        return buildLoginVO(user);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        blacklist(accessToken);
        blacklist(refreshToken);
        log.info("用户退出登录: userId={}", UserContext.userIdOrNull());
    }

    @Override
    public UserVO currentUser() {
        User user = getById(UserContext.userId());
        BusinessException.throwIf(user == null, ErrorCode.USER_NOT_FOUND);
        return UserVO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(UpdateProfileDTO dto) {
        User user = getById(UserContext.userId());
        BusinessException.throwIf(user == null, ErrorCode.USER_NOT_FOUND);

        // 只允许修改自己的这几个字段：用户名、密码、角色、状态都不在此接口
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getAvatar() != null && !dto.getAvatar().isBlank()) {
            user.setAvatar(dto.getAvatar().trim());
        }
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        updateById(user);
        return UserVO.from(getById(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        User user = getById(UserContext.userId());
        BusinessException.throwIf(user == null, ErrorCode.USER_NOT_FOUND);
        BusinessException.throwIf(!passwordEncoder.matches(dto.getOldPassword(), user.getPassword()),
                ErrorCode.OLD_PASSWORD_ERROR);

        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(update);

        log.info("用户修改密码: userId={}", user.getId());
    }

    /* ------------------------------------------------------------------ */

    private LoginVO buildLoginVO(User user) {
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.createRefreshToken(user.getId());
        return LoginVO.of(accessToken, refreshToken, jwtUtil.getAccessExpire(), UserVO.from(user));
    }

    /** 把 token 加入黑名单，TTL = 该 token 剩余有效期（已过期则忽略） */
    private void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            long remain = jwtUtil.getRemainingSeconds(claims);
            if (remain > 0) {
                redisTemplate.opsForValue().set(Constants.KEY_TOKEN_BLACKLIST + token, "1", Duration.ofSeconds(remain));
            }
        } catch (BusinessException e) {
            // token 已过期或非法，无需加入黑名单
            log.debug("退出登录时 token 已失效，跳过黑名单: {}", e.getMessage());
        }
    }
}
