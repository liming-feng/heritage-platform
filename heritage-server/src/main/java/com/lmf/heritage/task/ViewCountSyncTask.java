package com.lmf.heritage.task;

import com.lmf.heritage.common.Constants;
import com.lmf.heritage.module.content.mapper.ArticleMapper;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.BiFunction;

/**
 * 浏览量回写定时任务
 *
 * <p>商品 / 课程 / 资讯的浏览在详情接口里只做 Redis {@code INCR}
 * （{@code goods:view:{id}}、{@code course:view:{id}}、{@code article:view:{id}}），
 * 不碰数据库 —— 避免高并发浏览把 MySQL 打满。本任务每小时把累计增量批量刷回 MySQL。</p>
 *
 * <p><b>为什么回写后用 DECRBY 而不是直接删 key：</b>
 * 删 key 会把「从取数到删除之间新产生的浏览」一并丢掉；
 * 先读出 delta、回写、再 DECRBY(delta)，回写期间新增的浏览量会留在计数器里，等下一轮同步，不会丢。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private final StringRedisTemplate redis;
    private final GoodsMapper goodsMapper;
    private final CourseMapper courseMapper;
    private final ArticleMapper articleMapper;

    /** 每小时整点执行 */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncViewCounts() {
        long start = System.currentTimeMillis();
        int goodsCount = syncByPrefix(Constants.KEY_GOODS_VIEW,
                (id, delta) -> goodsMapper.addViewCount(id, delta), "商品");
        int courseCount = syncByPrefix(Constants.KEY_COURSE_VIEW,
                (id, delta) -> courseMapper.addViewCount(id, delta), "课程");
        int articleCount = syncByPrefix(Constants.KEY_ARTICLE_VIEW,
                (id, delta) -> articleMapper.addViewCount(id, delta), "资讯");
        log.info("[定时任务] 浏览量回写完成: 商品 {} 个 / 课程 {} 门 / 资讯 {} 篇, 耗时 {} ms",
                goodsCount, courseCount, articleCount, System.currentTimeMillis() - start);
    }

    /**
     * 按 key 前缀扫描计数器并回写
     *
     * <p>这里用的是 {@code KEYS}，对教学规模的数据量足够；若 key 数量上到十万级应换成 {@code SCAN} 游标遍历。</p>
     *
     * @param prefix 计数器 key 前缀，后缀为目标 id
     * @param writer 回写函数：(id, delta) -> 受影响行数
     */
    private int syncByPrefix(String prefix, BiFunction<Long, Long, Integer> writer, String label) {
        Set<String> keys = redis.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) {
            log.debug("[定时任务] {} 无待回写的浏览量", label);
            return 0;
        }

        int synced = 0;
        for (String key : keys) {
            String value = redis.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                redis.delete(key);
                continue;
            }

            long delta;
            long id;
            try {
                delta = Long.parseLong(value);
                id = Long.parseLong(key.substring(prefix.length()));
            } catch (NumberFormatException e) {
                // 脏 key（格式不对），清掉避免每轮都报错
                log.warn("[定时任务] {} 计数器格式异常，已清除: key={}, value={}", label, key, value);
                redis.delete(key);
                continue;
            }

            if (delta <= 0) {
                continue;
            }

            int rows = writer.apply(id, delta);
            if (rows > 0) {
                // 回写成功：只减掉已同步的部分，期间新增的浏览量保留到下一轮
                redis.opsForValue().decrement(key, delta);
                synced++;
            } else {
                // 目标已被删除，计数器没有意义了
                redis.delete(key);
                log.warn("[定时任务] {} id={} 不存在（可能已删除），已清除其浏览量计数器", label, id);
            }
        }
        return synced;
    }
}
