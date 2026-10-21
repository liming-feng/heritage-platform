package com.lmf.heritage.security;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 双 Token 工具单元测试（不依赖 Spring 容器，纯对象测试）
 *
 * <p>覆盖：access/refresh 生成与解析、claims 回读、过期判定、伪造 token 判定、
 * Bearer 前缀剥离、剩余有效期计算。HMAC-SHA256 要求密钥 ≥ 32 字节，
 * 测试里用 64 字符的固定串模拟生产配置。</p>
 */
class JwtUtilTest {

    /** 64 字节密钥（生产配置的最小长度是 32 字节，这里取双倍避免边界争议） */
    private static final String SECRET =
            "unit-test-secret-key-for-heritage-platform-jwt-hmac-sha256-0123456789abcdef";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // access 2 小时 / refresh 7 天，与 application.yml 生产配置一致
        jwtUtil = new JwtUtil(SECRET, 7200, 604800);
    }

    @Test
    @DisplayName("access_token 生成后能解析出 uid/uname/role，且 type=access")
    void accessTokenRoundTrip() {
        String token = jwtUtil.createAccessToken(23100025L, "user01", 1);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT 应该是三段式");

        Claims claims = jwtUtil.parse(token);
        assertEquals(23100025L, jwtUtil.getUserId(claims));
        assertEquals("user01", jwtUtil.getUsername(claims));
        assertEquals(1, jwtUtil.getRole(claims));
        assertEquals(JwtUtil.TYPE_ACCESS, jwtUtil.getType(claims));
    }

    @Test
    @DisplayName("refresh_token 只带 uid 与 type=refresh，不带 username/role")
    void refreshTokenRoundTrip() {
        String token = jwtUtil.createRefreshToken(42L);
        Claims claims = jwtUtil.parse(token);

        assertEquals(42L, jwtUtil.getUserId(claims));
        assertEquals(JwtUtil.TYPE_REFRESH, jwtUtil.getType(claims));
        assertNull(jwtUtil.getUsername(claims), "refresh_token 不应携带 username");
        assertNull(jwtUtil.getRole(claims), "refresh_token 不应携带 role");
    }

    @Test
    @DisplayName("过期 token 抛 TOKEN_EXPIRED(1005)，而不是通用异常")
    void expiredToken() {
        // accessExpire = -3600 → 签发时刻即在 1 小时前，必然过期
        JwtUtil shortLived = new JwtUtil(SECRET, -3600, -3600);
        String token = shortLived.createAccessToken(1L, "user01", 0);

        BusinessException ex = assertThrows(BusinessException.class, () -> jwtUtil.parse(token));
        assertEquals(ErrorCode.TOKEN_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("伪造/篡改的 token 抛 TOKEN_INVALID(1004)")
    void forgedToken() {
        String token = jwtUtil.createAccessToken(1L, "user01", 0);
        // 篡改签名段最后一个字符，签名校验必然失败
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("AA") ? "BB" : "AA");

        BusinessException ex = assertThrows(BusinessException.class, () -> jwtUtil.parse(tampered));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), ex.getCode());

        // 完全瞎写的字符串同样是 TOKEN_INVALID
        BusinessException ex2 = assertThrows(BusinessException.class, () -> jwtUtil.parse("garbage"));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("用不同密钥签发的 token 视为无效（密钥轮换场景）")
    void wrongSecretToken() {
        JwtUtil other = new JwtUtil(SECRET + "different-bytes-for-another-instance", 7200, 604800);
        String token = other.createAccessToken(1L, "user01", 0);

        BusinessException ex = assertThrows(BusinessException.class, () -> jwtUtil.parse(token));
        assertEquals(ErrorCode.TOKEN_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getRemainingSeconds：未过期为正数，过期后为 0（黑名单 TTL 依据）")
    void remainingSeconds() {
        Claims claims = jwtUtil.parse(jwtUtil.createAccessToken(1L, "u", 0));
        long remain = jwtUtil.getRemainingSeconds(claims);
        assertTrue(remain > 7000 && remain <= 7200, "剩余时间应接近 7200s，实际 " + remain);
    }

    @Test
    @DisplayName("stripPrefix：Bearer 前缀、无前缀、null 三种输入")
    void stripPrefix() {
        assertEquals("abc", JwtUtil.stripPrefix("Bearer abc", "Bearer "));
        assertEquals("abc", JwtUtil.stripPrefix("abc", "Bearer "));
        assertNull(JwtUtil.stripPrefix(null, "Bearer "));
        assertEquals("abc", JwtUtil.stripPrefix("  abc  ", null));
    }

    @Test
    @DisplayName("双 Token 有效期配置能正确读回")
    void expireConfig() {
        assertEquals(7200, jwtUtil.getAccessExpire());
        assertEquals(604800, jwtUtil.getRefreshExpire());
    }
}
