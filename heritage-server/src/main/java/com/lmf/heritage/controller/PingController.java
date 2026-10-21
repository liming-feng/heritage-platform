package com.lmf.heritage.controller;

import com.lmf.heritage.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口（P0 阶段用于验证 context-path、跨域、MySQL、Redis 是否全部打通）
 */
@Tag(name = "00-健康检查", description = "环境连通性自检")
@RestController
@RequestMapping("/portal")
public class PingController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${spring.application.name:heritage-server}")
    private String appName;

    public PingController(JdbcTemplate jdbcTemplate, StringRedisTemplate stringRedisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Operation(summary = "连通性自检", description = "返回应用名、MySQL 版本、Redis PING 结果")
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", appName);
        data.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        String mysql = "未连接";
        try {
            mysql = jdbcTemplate.queryForObject("select version()", String.class);
        } catch (Exception e) {
            mysql = "连接失败: " + e.getMessage();
        }
        data.put("mysql", mysql);

        String redis = "未连接";
        try {
            redis = "PONG".equalsIgnoreCase(stringRedisTemplate.execute(
                    (org.springframework.data.redis.core.RedisCallback<String>) connection ->
                            connection.ping())) ? "PONG" : "异常";
        } catch (Exception e) {
            redis = "连接失败: " + e.getMessage();
        }
        data.put("redis", redis);
        return Result.success(data);
    }
}
