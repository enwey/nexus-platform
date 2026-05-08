package com.nexus.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigCorsIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(PlatformCorsProperties.class, WebConfig.class);

    @Test
    void shouldBuildCorsConfigurationFromProperties() {
        contextRunner
                .withPropertyValues(
                        "platform.cors.allowed-origin-patterns=https://ops.example.com,https://dev.example.com",
                        "platform.cors.allowed-methods=GET,POST,PATCH",
                        "platform.cors.allowed-headers=Authorization,Content-Type,X-Idempotency-Key",
                        "platform.cors.allow-credentials=false",
                        "platform.cors.max-age-seconds=7200"
                )
                .run(context -> {
                    CorsConfigurationSource source = context.getBean(CorsConfigurationSource.class);
                    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/admin/ops/reviews");
                    request.addHeader(HttpHeaders.ORIGIN, "https://ops.example.com");
                    CorsConfiguration configuration = source.getCorsConfiguration(request);

                    assertThat(configuration).isNotNull();
                    assertThat(configuration.getAllowedOriginPatterns())
                            .containsExactly("https://ops.example.com", "https://dev.example.com");
                    assertThat(configuration.getAllowedMethods())
                            .containsExactly("GET", "POST", "PATCH");
                    assertThat(configuration.getAllowedHeaders())
                            .containsExactly("Authorization", "Content-Type", "X-Idempotency-Key");
                    assertThat(configuration.getAllowCredentials()).isFalse();
                    assertThat(configuration.getMaxAge()).isEqualTo(7200L);
                });
    }
}
