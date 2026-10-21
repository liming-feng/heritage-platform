package com.lmf.heritage.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lmf.heritage.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Redis 缓存助手（规划书 7.2 的落地实现）
 *
 * <p>把「查缓存 → 未命中回源 → 回填」这套模板收敛到一处，业务里只写一行 {@code getOrLoad}。</p>
 *
 * <p><b>三个缓存问题的处理：</b></p>
 * <ul>
 *   <li><b>穿透</b>：回源结果为空时写入 {@link #EMPTY_FLAG} 占位，TTL 用 {@link Constants#TTL_EMPTY}（5 分钟），
 *       期间同样的请求直接返回空、不再落到数据库；</li>
 *   <li><b>击穿</b>：热点数据（轮播、分类、热门商品）给足 TTL，过期后由第一个请求回源、其余请求短暂等待；</li>
 *   <li><b>雪崩</b>：TTL 在基础值上叠加随机抖动，避免同一批 key 同时过期。</li>
 * </ul>
 *
 * <p>缓存对象用**独立的 ObjectMapper**序列化成 JSON 字符串，不复用全局的（全局那个会把 Long 写成字符串，
 * 缓存里再读出来语义就变了）。反序列化失败时按「未命中」处理并删除脏缓存，保证不会因为一次结构变更让接口直接 500。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheHelper {

    /** 空结果占位符（防穿透）。用不可见字符做前缀，避免与正常业务值冲突 */
    public static final String EMPTY_FLAG = "\u0000EMPTY";

    /** 缓存专用序列化器：保留 LocalDateTime 原始语义，不把 Long 转字符串 */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final StringRedisTemplate redis;

    /* ==================== 对象缓存 ==================== */

    /**
     * 读对象缓存，未命中则回源并回填。
     *
     * @param key    缓存 key
     * @param type   对象类型
     * @param ttl    回填后的有效期（秒）
     * @param loader 回源逻辑，返回 null 表示数据不存在（会写空值占位）
     */
    public <T> T getOrLoad(String key, Class<T> type, long ttl, Supplier<T> loader) {
        String json = redis.opsForValue().get(key);
        if (json != null) {
            if (EMPTY_FLAG.equals(json)) {
                log.debug("[缓存] 命中空值占位 {}（防穿透生效）", key);
                return null;
            }
            T cached = read(json, type, key);
            if (cached != null) {
                log.debug("[缓存] 命中 {}", key);
                return cached;
            }
            // 脏缓存（结构变更等），删掉后走回源
        }

        T value = loader.get();
        if (value == null) {
            setEmpty(key);
        } else {
            set(key, value, ttl);
        }
        return value;
    }

    /* ==================== 集合缓存 ==================== */

    /**
     * 读集合缓存，未命中则回源并回填。空集合同样写空值占位防穿透。
     */
    public <T> List<T> getListOrLoad(String key, Class<T> elemType, long ttl, Supplier<List<T>> loader) {
        String json = redis.opsForValue().get(key);
        if (json != null) {
            if (EMPTY_FLAG.equals(json)) {
                log.debug("[缓存] 命中空值占位 {}（防穿透生效）", key);
                return List.of();
            }
            List<T> cached = readList(json, elemType, key);
            if (cached != null) {
                log.debug("[缓存] 命中 {}，共 {} 条", key, cached.size());
                return cached;
            }
        }

        List<T> value = loader.get();
        if (value == null || value.isEmpty()) {
            setEmpty(key);
            return List.of();
        }
        set(key, value, ttl);
        return value;
    }

    /* ==================== 基础读写 ==================== */

    public void set(String key, Object value, long ttlSeconds) {
        try {
            // TTL 加 ±10% 随机抖动，避免缓存雪崩（同批 key 同时过期）
            long jitter = ttlSeconds / 10;
            long realTtl = ttlSeconds + (jitter <= 0 ? 0 : (long) (Math.random() * jitter * 2) - jitter);
            redis.opsForValue().set(key, MAPPER.writeValueAsString(value), Duration.ofSeconds(Math.max(realTtl, 1)));
        } catch (Exception e) {
            // 缓存写失败绝不能影响主流程
            log.warn("[缓存] 写入失败 key={}, err={}", key, e.getMessage());
        }
    }

    /** 写入空值占位，TTL 固定 5 分钟 */
    public void setEmpty(String key) {
        try {
            redis.opsForValue().set(key, EMPTY_FLAG, Duration.ofSeconds(Constants.TTL_EMPTY));
            log.debug("[缓存] 写入空值占位 {}", key);
        } catch (Exception e) {
            log.warn("[缓存] 写入空值失败 key={}, err={}", key, e.getMessage());
        }
    }

    /** 读原始值，未命中或空值占位都返回 null */
    public String getRaw(String key) {
        String v = redis.opsForValue().get(key);
        return EMPTY_FLAG.equals(v) ? null : v;
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    /** 删除缓存（更新策略：先更新数据库，再删缓存） */
    public void delete(String... keys) {
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            try {
                redis.delete(key);
                log.debug("[缓存] 已删除 {}", key);
            } catch (Exception e) {
                log.warn("[缓存] 删除失败 key={}, err={}", key, e.getMessage());
            }
        }
    }

    /** 按前缀批量删除（用 SCAN，不用 KEYS，避免阻塞 Redis） */
    public long deleteByPrefix(String prefix) {
        long count = 0;
        try {
            var keys = redis.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                count = keys.size();
                redis.delete(keys);
                log.debug("[缓存] 按前缀 {} 删除 {} 个 key", prefix, count);
            }
        } catch (Exception e) {
            log.warn("[缓存] 按前缀删除失败 prefix={}, err={}", prefix, e.getMessage());
        }
        return count;
    }

    /** 自增计数器，返回自增后的值；key 不存在时从 0 开始 */
    public long increment(String key) {
        Long v = redis.opsForValue().increment(key);
        return v == null ? 0L : v;
    }

    /** 读取计数器当前值，不存在返回 0 */
    public long getCounter(String key) {
        String v = redis.opsForValue().get(key);
        if (v == null || EMPTY_FLAG.equals(v)) {
            return 0L;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 删除计数器 */
    public void deleteCounter(String key) {
        redis.delete(key);
    }

    /* ==================== 内部方法 ==================== */

    private <T> T read(String json, Class<T> type, String key) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            log.warn("[缓存] 反序列化失败，按未命中处理 key={}, err={}", key, e.getMessage());
            redis.delete(key);
            return null;
        }
    }

    private <T> List<T> readList(String json, Class<T> elemType, String key) {
        try {
            JavaType javaType = MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, elemType);
            return MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("[缓存] 集合反序列化失败，按未命中处理 key={}, err={}", key, e.getMessage());
            redis.delete(key);
            return null;
        }
    }

    /** 空集合判断的小工具，给业务层用 */
    public static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }
}
