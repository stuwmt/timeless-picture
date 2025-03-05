package com.timeless.timelesspicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.timeless.timelesspicturebackend.mapper")
@EnableAsync // 开启异步
@EnableAspectJAutoProxy(exposeProxy = true) // 开启AOP代理
public class TimelessPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimelessPictureBackendApplication.class, args);
    }

}
