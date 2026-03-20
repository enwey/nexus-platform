package com.nexus.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nexus Platform 主应用程序类
 * 基于Spring Boot的游戏平台后端服务
 */
@SpringBootApplication
public class NexusPlatformApplication {
    /**
     * 应用程序入口点
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NexusPlatformApplication.class, args);
    }
}
