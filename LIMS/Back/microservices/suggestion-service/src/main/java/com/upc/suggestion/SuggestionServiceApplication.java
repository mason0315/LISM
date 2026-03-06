package com.upc.suggestion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.upc.common.feign")
@MapperScan("com.upc.suggestion.mapper")
public class SuggestionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuggestionServiceApplication.class, args);
    }
}
