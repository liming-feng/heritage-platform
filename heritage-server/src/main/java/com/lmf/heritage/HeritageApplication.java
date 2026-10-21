package com.lmf.heritage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 非遗手工艺品展示与预约体验平台 - 启动类
 *
 * @author 李明峰 23100025
 */
@SpringBootApplication
@MapperScan("com.lmf.heritage.module.**.mapper")
@EnableScheduling
@EnableAsync
public class HeritageApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeritageApplication.class, args);
        System.out.println("""

                ==========================================================
                  非遗手工艺品展示与预约体验平台 启动成功
                  接口文档: http://localhost:8080/api/doc.html
                  健康检查: http://localhost:8080/api/portal/ping
                ==========================================================
                """);
    }
}
