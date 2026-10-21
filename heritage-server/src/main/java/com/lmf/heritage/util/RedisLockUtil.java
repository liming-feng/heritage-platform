package com.lmf.heritage.util;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Redis 分布式锁（手写实现，不依赖 Redisson）
 *
 * <pre>
 * 加锁：SET key requestId NX PX leaseMillis
 *       —— NX 保证互斥，PX 保证宕机后锁能自动释放
 * 释放：Lua 脚本「先比对 requestId 再 DEL」
 *       —— 保证「判断 + 删除」的原子性，避免误删他人持有的锁
 * </pre>
 *
 * <p>本类是「预约防超卖三层防护」的第一层：保证同一课程同一时刻
 * 只有一个请求进入名额校验，把并发压力挡在数据库之前。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 释放锁的 Lua 脚本：
     * 只有当锁的 value 与当前请求的 requestId 一致时才删除，
     * 并且返回删除结果，保证原子性。
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('del', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end",
            Long.class);

    /** 默认锁持有时间 10 秒 */
    private static final long DEFAULT_LEASE_MILLIS = 10_000L;
    /** 默认自旋等待时间 3 秒 */
    private static final long DEFAULT_WAIT_MILLIS = 3_000L;
    /**
     * 自旋重试间隔。
     *
     * <p>10ms 是权衡后的取值：间隔太大（如 50ms）会让「100 并发抢同一门课」
     * 的排队时间被无谓地拉长——持锁者几毫秒就释放了，等锁的人却还要睡满一个间隔，
     * 100 个请求累计就多出好几秒，最后一批直接等超时返回「系统繁忙」；
     * 间隔太小则会给 Redis 带来无意义的空转请求。10ms 下总请求量依然可忽略。</p>
     */
    private static final long SPIN_INTERVAL_MILLIS = 10L;

    /**
     * 尝试加锁（不等待，加不到立即返回 false）
     *
     * @param key         锁的 key
     * @param requestId   持有者标识，解锁时要回传
     * @param leaseMillis 锁自动过期时间（毫秒）
     */
    public boolean tryLock(String key, String requestId, long leaseMillis) {
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(key, requestId, Duration.ofMillis(leaseMillis));
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 加锁（自旋等待，超时仍拿不到则返回 false）
     */
    public boolean lock(String key, String requestId, long waitMillis, long leaseMillis) {
        long deadline = System.currentTimeMillis() + waitMillis;
        do {
            if (tryLock(key, requestId, leaseMillis)) {
                return true;
            }
            try {
                Thread.sleep(SPIN_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    /**
     * 释放锁（Lua 原子校验 + 删除）
     */
    public boolean unlock(String key, String requestId) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), requestId);
        return result != null && result > 0;
    }

    /**
     * 模板方法：加锁 -> 执行 -> finally 解锁
     *
     * @param key      锁 key
     * @param supplier 临界区逻辑
     * @param busyCode 抢不到锁时抛出的错误码
     */
    public <T> T executeWithLock(String key, Supplier<T> supplier, ErrorCode busyCode) {
        String requestId = UUID.randomUUID().toString();
        if (!lock(key, requestId, DEFAULT_WAIT_MILLIS, DEFAULT_LEASE_MILLIS)) {
            log.warn("获取分布式锁失败: {}", key);
            throw new BusinessException(busyCode);
        }
        try {
            return supplier.get();
        } finally {
            // 无论业务成功或抛异常，都要释放锁
            unlock(key, requestId);
        }
    }

    /** 便捷重载：默认抢不到锁返回「系统繁忙」 */
    public <T> T executeWithLock(String key, Supplier<T> supplier) {
        return executeWithLock(key, supplier, ErrorCode.BOOKING_QUEUE_BUSY);
    }

    /** 便捷重载：无返回值 */
    public void executeWithLock(String key, Runnable runnable) {
        executeWithLock(key, () -> {
            runnable.run();
            return null;
        });
    }
}
