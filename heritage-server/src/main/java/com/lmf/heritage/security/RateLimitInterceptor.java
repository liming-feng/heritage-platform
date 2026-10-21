package com.lmf.heritage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.heritage.annotation.RateLimit;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 接口限流拦截器：Redis 计数器 + 固定窗口
 *
 * <p>仅在方法上标注 &#64;RateLimit 时生效，如登录、下单、预约提交。</p>
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RateLimit anno = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (anno == null) {
            return true;
        }

        String dimensionKey = switch (anno.dimension()) {
            case "user" -> "u" + (UserContext.userIdOrNull() == null ? IpUtil.getIp(request) : UserContext.userIdOrNull());
            case "global" -> "g";
            default -> "i" + IpUtil.getIp(request);
        };

        String key = Constants.KEY_RATE_LIMIT + request.getRequestURI() + ":" + dimensionKey;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(anno.window()));
        }
        if (count != null && count > anno.count()) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            Result<Void> body = Result.error(ErrorCode.REPEAT_SUBMIT, anno.message());
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }
        return true;
    }
}
