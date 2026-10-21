package com.lmf.heritage.config;

import com.lmf.heritage.security.JwtInterceptor;
import com.lmf.heritage.security.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：注册拦截器 + 静态资源映射
 *
 * <p><b>路径规则（重要）：</b>context-path 已经是 <code>/api</code>，
 * 因此这里的 pathPatterns 一律<b>不含</b> /api，只写 /portal、/craftsman、/admin 三段。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Value("${heritage.upload.path}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/portal/**", "/craftsman/**", "/admin/**")
                .order(1);

        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }

    /**
     * 图片等上传文件的静态资源映射
     * 对外访问形如 http://localhost:8080/api/uploads/2026/09/18/xxx.png
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadPath;
        if (!location.startsWith("file:")) {
            location = "file:" + location;
        }
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);

        // Knife4j / Swagger UI 静态资源
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
