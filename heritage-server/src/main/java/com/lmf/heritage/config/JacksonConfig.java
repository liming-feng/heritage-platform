package com.lmf.heritage.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 配置
 *
 * <pre>
 *  1. LocalDateTime 统一按 yyyy-MM-dd HH:mm:ss 输出与接收
 *  2. Long 序列化为字符串，避免前端 JS 大整数精度丢失（雪花 ID / 自增 ID 都适用）
 * </pre>
 *
 * <p><b>为什么不自己 new 一个 ObjectMapper 作为 Bean？</b>
 * Spring Boot 的 {@code JacksonAutoConfiguration} 已经提供了一个 ObjectMapper，
 * 并且它会收集容器里所有 {@link Jackson2ObjectMapperBuilderCustomizer}
 * （Boot 自带的 standardJacksonObjectMapperBuilderCustomizer + 本类这个），
 * 一起作用于它自己构建的 ObjectMapper。</p>
 *
 * <p>若再手写一个 {@code @Bean ObjectMapper(...)}，一旦其入参是
 * {@code Jackson2ObjectMapperBuilderCustomizer}，Spring 会找到 2 个候选而报
 * "required a single bean, but 2 were found" 导致启动失败。
 * 因此这里<b>只</b>提供 customizer，不提供 ObjectMapper Bean。</p>
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE = "yyyy-MM-dd";
    private static final String TIME = "HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer heritageJacksonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("heritageJacksonModule");

            module.addSerializer(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME)));
            module.addDeserializer(LocalDateTime.class,
                    new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME)));

            module.addSerializer(LocalDate.class,
                    new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE)));
            module.addDeserializer(LocalDate.class,
                    new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE)));

            module.addSerializer(LocalTime.class,
                    new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME)));
            module.addDeserializer(LocalTime.class,
                    new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME)));

            // Long -> String，防止前端 JS 精度丢失
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);

            // 先注册 JavaTimeModule（保证 Instant/Duration 等其他 java.time 类型可用），
            // 再注册自定义 module —— Jackson 中后注册的 module 优先级更高，
            // 因此 LocalDateTime/LocalDate/LocalTime 会按上面的格式输出。
            builder.modules(new JavaTimeModule(), module);

            builder.simpleDateFormat(DATE_TIME);
            builder.failOnUnknownProperties(false);
            builder.failOnEmptyBeans(false);
        };
    }
}
