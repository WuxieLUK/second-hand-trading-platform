package com.itheima;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.itheima.mapper")
@SpringBootApplication
@EnableCaching
@EnableScheduling // 开启定时任务
public class BigEventApplication{
    public static void main(String[] args) {
        SpringApplication.run(BigEventApplication.class, args);
    }

}