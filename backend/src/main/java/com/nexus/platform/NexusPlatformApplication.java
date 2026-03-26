package com.nexus.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NexusPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusPlatformApplication.class, args);
    }
}
