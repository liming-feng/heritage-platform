package com.lmf.heritage.util;

import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 用户启停状态解析（带 Redis 缓存）
 *
 * <p><b>为什么需要它</b>：管理端「禁用用户」如果只在数据库里改一个字段，
 * 那个用户手里已经签发的 access_token 在过期前（2 小时）照样能访问所有接口——
 * 所谓「禁用」就成了半成品。要让它立刻生效，就得在每个请求上判一次状态；
 * 而每个请求查一次 user 表，代价是每访问一个接口就多一次数据库往返。</p>
 *
 * <p><b>折中做法</b>：状态缓存在 Redis（10 分钟）。三个写入点构成闭环——</p>
 * <ul>
 *   <li>管理端点「禁用/启用」时立即写这个键 → 下一个请求就命中新状态，零延迟；</li>
 *   <li>缓存未命中时回源查库并回填 → 冷启动只付一次代价；</li>
 *   <li>读不到用户（被删除）按「禁用」处理 → 宁可拒绝，也不要放过一个已注销账号。</li>
 * </ul>
 *
 * <p>不用「把 token 拉黑」的方案，是因为禁用是<b>按人</b>而不是<b>按令牌</b>的：
 * 管理员禁的是一个账号，而这个账号可能已经在手机、电脑上登录过多次，
 * 逐个拉黑既拉不全，也拉不准。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusHelper {

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    /** 状态值：允许访问 */
    public static final String ENABLED = "1";

    /** 状态值：拒绝访问 */
    public static final String DISABLED = "0";

    /**
     * 该账号是否已被禁用
     *
     * <p>查不到用户也返回 true（视为禁用）：能走到这一步说明令牌是有效的，
     * 而令牌对应的账号却不存在了，唯一合理的处理就是拒绝。</p>
     */
    public boolean isDisabled(Long userId) {
        if (userId == null) {
            return true;
        }
        String key = Constants.KEY_USER_STATUS + userId;
        String cached = safeGet(key);
        if (cached != null) {
            return DISABLED.equals(cached);
        }

        User user = userMapper.selectById(userId);
        String value = (user == null || !Integer.valueOf(Constants.STATUS_ENABLED).equals(user.getStatus()))
                ? DISABLED : ENABLED;
        safeSet(key, value);
        return DISABLED.equals(value);
    }

    /**
     * 管理端改动状态后刷新缓存
     *
     * <p>写入而不是删除：删除会让「禁用生效」退化成「下一个请求才回源查库」，
     * 虽然结果一样，但并发下会有一小段窗口里多个请求同时回源。
     * 直接写目标值可以让生效是确定性的。</p>
     */
    public void refresh(Long userId, Integer status) {
        if (userId == null) {
            return;
        }
        String value = Integer.valueOf(Constants.STATUS_ENABLED).equals(status) ? ENABLED : DISABLED;
        safeSet(Constants.KEY_USER_STATUS + userId, value);
    }

    /** 缓存删掉（用户被物理删除等场景） */
    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisTemplate.delete(Constants.KEY_USER_STATUS + userId);
        } catch (Exception e) {
            log.warn("[用户状态] 缓存清除失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /* Redis 不可用时降级为「不阻断」：可用性优先于这条附加校验 */

    private String safeGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("[用户状态] 缓存读取失败，降级回源：{}", e.getMessage());
            return null;
        }
    }

    private void safeSet(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(Constants.TTL_USER_STATUS));
        } catch (Exception e) {
            log.warn("[用户状态] 缓存写入失败 {}={}：{}", key, value, e.getMessage());
        }
    }
}
