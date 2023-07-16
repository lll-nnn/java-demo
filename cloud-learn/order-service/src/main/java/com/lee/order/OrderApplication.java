package com.lee.order;

import com.lee.feign.config.DefaultFeignConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.lee.order.mapper")
@EnableFeignClients(basePackages = "com.lee.feign.clients",defaultConfiguration = DefaultFeignConfiguration.class)//设置feign配置文件    对全局
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
