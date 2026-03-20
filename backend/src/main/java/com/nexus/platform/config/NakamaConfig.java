package com.nexus.platform.config;

import io.nakama.apiclient.ApiClient;
import io.nakama.apiclient.DefaultClient;
import io.nakama.apiclient.SessionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NakamaConfig {

    @Value("${nakama.server.key}")
    private String serverKey;

    @Value("${nakama.http.key}")
    private String httpKey;

    @Value("${nakama.server.url}")
    private String serverUrl;

    @Bean
    public ApiClient nakamaApiClient() {
        return DefaultClient.builder()
                .serverKey(serverKey)
                .host(serverUrl)
                .build();
    }

    @Bean
    public SessionClient nakamaSessionClient() {
        return DefaultClient.builder()
                .serverKey(serverKey)
                .host(serverUrl)
                .build();
    }
}
