package com.upc.shelves;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.upc.common.feign")
@MapperScan("com.upc.shelves.mapper")
public class ShelvesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShelvesServiceApplication.class, args);
    }
}
