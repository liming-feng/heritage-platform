package com.lmf.heritage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 *
 * <p>使用 BCrypt（自带随机盐，同一明文每次加密结果不同），
 * 数据库里存的是密文，任何接口都不会把 password 返回给前端。</p>
 *
 * <p>注意：这里只依赖了 spring-security-crypto 这一个 jar，
 * <b>没有</b>引入 spring-boot-starter-security，所以不会触发
 * SpringSecurity 的默认登录页与 Basic 认证，接口仍然是自由访问的。</p>
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
