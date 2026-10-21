package com.lmf.heritage.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置：按三端分组
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "Authorization";

    @Bean
    public OpenAPI heritageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("非遗手工艺品展示与预约体验平台 API")
                        .description("基于 SpringBoot 3.2.5 + Vue 3.4 · 用户端 / 匠人端 / 管理员端三端接口")
                        .version("1.0.0")
                        .contact(new Contact().name("李明峰 23100025").email("23100025@example.com")))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }

    @Bean
    public GroupedOpenApi portalApi() {
        return GroupedOpenApi.builder().group("1-用户端").pathsToMatch("/portal/**").build();
    }

    @Bean
    public GroupedOpenApi craftsmanApi() {
        return GroupedOpenApi.builder().group("2-匠人端").pathsToMatch("/craftsman/**").build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("3-管理端").pathsToMatch("/admin/**").build();
    }

    @Bean
    public GroupedOpenApi commonApi() {
        return GroupedOpenApi.builder().group("0-公共").pathsToMatch("/common/**").build();
    }
}
