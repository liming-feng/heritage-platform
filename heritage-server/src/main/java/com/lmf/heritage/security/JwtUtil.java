package com.lmf.heritage.security;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 双 Token 工具（JJWT 0.12.6 新 API）
 *
 * <pre>
 * access_token  ：2 小时，携带 userId / username / role，用于业务接口鉴权
 * refresh_token ：7 天，只携带 userId，仅用于换发新的 access_token
 * </pre>
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_USERNAME = "uname";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;

    /** access_token 有效期（秒） */
    @Getter
    private final long accessExpire;

    /** refresh_token 有效期（秒） */
    @Getter
    private final long refreshExpire;

    public JwtUtil(@Value("${heritage.jwt.secret}") String secret,
                   @Value("${heritage.jwt.access-expire:7200}") long accessExpire,
                   @Value("${heritage.jwt.refresh-expire:604800}") long refreshExpire) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpire = accessExpire;
        this.refreshExpire = refreshExpire;
    }

    /** 生成 access_token */
    public String createAccessToken(Long userId, String username, Integer role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        return build(claims, accessExpire);
    }

    /** 生成 refresh_token */
    public String createRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
        return build(claims, refreshExpire);
    }

    private String build(Map<String, Object> claims, long expireSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token
     *
     * @throws BusinessException TOKEN_EXPIRED / TOKEN_INVALID
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    public Long getUserId(Claims claims) {
        Object v = claims.get(CLAIM_USER_ID);
        return v == null ? null : Long.valueOf(v.toString());
    }

    public String getUsername(Claims claims) {
        Object v = claims.get(CLAIM_USERNAME);
        return v == null ? null : v.toString();
    }

    public Integer getRole(Claims claims) {
        Object v = claims.get(CLAIM_ROLE);
        return v == null ? null : Integer.valueOf(v.toString());
    }

    public String getType(Claims claims) {
        Object v = claims.get(CLAIM_TYPE);
        return v == null ? null : v.toString();
    }

    /** token 剩余有效期（秒），用于黑名单 TTL；已过期返回 0 */
    public long getRemainingSeconds(Claims claims) {
        Date exp = claims.getExpiration();
        if (exp == null) {
            return 0;
        }
        long remain = (exp.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remain, 0);
    }

    /** 去掉 Bearer 前缀 */
    public static String stripPrefix(String header, String prefix) {
        if (header == null) {
            return null;
        }
        if (prefix != null && !prefix.isEmpty() && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        return header.trim();
    }
}
