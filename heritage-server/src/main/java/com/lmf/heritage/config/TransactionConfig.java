package com.lmf.heritage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 编程式事务模板
 *
 * <p><b>为什么需要它：</b>预约提交与定时任务的批量处理都需要「Redis 锁 / 循环迭代在事务之外，
 * 数据库操作在事务之内」。用 {@code @Transactional} 注解做不到这一点——注解的事务边界是
 * 整个方法，锁会被包在事务里，导致「事务还没提交锁就释放了」以及「等锁的线程白占数据库连接」。</p>
 *
 * <p>用 {@link TransactionTemplate} 把事务边界精确地放在该放的位置：
 * 先在事务外抢到锁，再开事务做数据库操作，事务提交后才释放锁。顺序更严谨。</p>
 *
 * @author 李明峰 23100025
 */
@Configuration
public class TransactionConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
