package com.lmf.heritage.util;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Redis 分布式锁单元测试（预约防超卖三层防护的第一层）
 *
 * <p>用 Mockito 模拟 StringRedisTemplate，验证锁语义本身：
 * NX 互斥、Lua 原子释放、自旋重试、超时放弃、模板方法的 finally 解锁。
 * 真实 Redis 下的 100 并发不超卖验证在 P5/P8 的 HTTP 并发测试与 P10 的 JMeter 压测中完成。</p>
 */
@ExtendWith(MockitoExtension.class)
class RedisLockUtilTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RedisLockUtil lockUtil;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lockUtil = new RedisLockUtil(redisTemplate);
    }

    /* ============ tryLock：NX 语义 ============ */

    @Test
    @DisplayName("tryLock：setIfAbsent 成功返回 true，失败（已被他人持有）返回 false")
    void tryLockNx() {
        when(valueOps.setIfAbsent(eq("lock:booking:1"), anyString(), eq(Duration.ofMillis(10_000))))
                .thenReturn(true).thenReturn(false);

        assertTrue(lockUtil.tryLock("lock:booking:1", "req-A", 10_000));
        assertFalse(lockUtil.tryLock("lock:booking:1", "req-B", 10_000),
                "同一 key 第二次加锁应失败（NX 互斥）");

        // 两个请求的 requestId 必须不同，否则解锁时无法区分持有者
        verify(valueOps, times(2)).setIfAbsent(eq("lock:booking:1"),
                argThat(id -> id != null && !id.isEmpty()), eq(Duration.ofMillis(10_000)));
    }

    @Test
    @DisplayName("tryLock：Redis 返回 null（如超时抖动）视为加锁失败而不是 NPE")
    void tryLockNullSafe() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(null);
        assertFalse(lockUtil.tryLock("k", "r", 1000));
    }

    /* ============ unlock：Lua 原子校验 ============ */

    @Test
    @DisplayName("unlock：Lua 脚本返回 1 视为释放成功，0（不是自己的锁）返回 false")
    void unlockAtomic() {
        when(redisTemplate.<Long>execute(any(RedisScript.class), eq(List.of("k")), eq("req-A")))
                .thenReturn(1L);
        when(redisTemplate.<Long>execute(any(RedisScript.class), eq(List.of("k")), eq("req-B")))
                .thenReturn(0L);

        assertTrue(lockUtil.unlock("k", "req-A"), "持有者本人释放应成功");
        assertFalse(lockUtil.unlock("k", "req-B"), "非持有者不能误删他人的锁");
    }

    /* ============ lock：自旋等待 ============ */

    @Test
    @DisplayName("lock：前几次失败后拿到锁（自旋重试生效）")
    void lockSpinsUntilSuccess() {
        AtomicInteger calls = new AtomicInteger();
        // 前 3 次加锁失败（返回 false），第 4 次成功——模拟锁被他人短暂持有
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> calls.incrementAndGet() > 3);

        // 3 秒等待窗口足够覆盖 3 次 10ms 间隔的自旋
        assertTrue(lockUtil.lock("k", "r", 3000, 10_000));
        assertEquals(4, calls.get(), "应自旋到第 4 次才成功，实际 " + calls.get());
    }

    @Test
    @DisplayName("lock：等待窗口内始终抢不到则返回 false，不抛异常")
    void lockTimesOut() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertFalse(lockUtil.lock("k", "r", 100, 10_000), "100ms 内抢不到应放弃");
    }

    /* ============ executeWithLock：模板方法 ============ */

    @Test
    @DisplayName("executeWithLock：拿到锁后执行业务，正常返回时释放锁")
    void executeWithLockHappyPath() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.<Long>execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        String result = lockUtil.executeWithLock("k", () -> "business-result");
        assertEquals("business-result", result);

        verify(redisTemplate, times(1)).<Long>execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    @DisplayName("executeWithLock：业务抛异常时锁也必须被释放（finally 保证）")
    void executeWithLockReleasesOnException() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.<Long>execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        assertThrows(IllegalStateException.class, () ->
                lockUtil.executeWithLock("k", () -> {
                    throw new IllegalStateException("业务炸了");
                }));

        verify(redisTemplate, times(1)).<Long>execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    @DisplayName("executeWithLock：自旋超时抢不到锁，抛 BOOKING_QUEUE_BUSY(5018) 且不执行业务")
    void executeWithLockBusy() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        AtomicInteger executed = new AtomicInteger();
        BusinessException ex = assertThrows(BusinessException.class, () ->
                lockUtil.executeWithLock("k", () -> {
                    executed.incrementAndGet();
                    return "never";
                }));

        assertEquals(ErrorCode.BOOKING_QUEUE_BUSY.getCode(), ex.getCode());
        assertEquals(0, executed.get(), "抢不到锁时业务逻辑不应被执行");
        verify(redisTemplate, never()).<Long>execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    @DisplayName("executeWithLock(Runnable)：无返回值重载正常执行并释放锁")
    void runnableOverload() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.<Long>execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        AtomicInteger ran = new AtomicInteger();
        assertDoesNotThrow(() -> lockUtil.executeWithLock("k", ran::incrementAndGet));
        assertEquals(1, ran.get());
    }
}
