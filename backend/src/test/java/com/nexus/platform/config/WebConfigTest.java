package com.nexus.platform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class WebConfigTest {

    @Test
    void corsFilterShouldUseConfiguredOriginsAndMethods() {
        PlatformCorsProperties properties = new PlatformCorsProperties();
        properties.setAllowedOriginPatterns(List.of("https://ops.nexus.example.com", "https://dev.nexus.example.com"));
        properties.setAllowedMethods(List.of("GET", "POST"));
        properties.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        properties.setAllowCredentials(true);
        properties.setMaxAgeSeconds(7200L);

        WebConfig webConfig = new WebConfig(properties);
        CorsConfigurationSource configurationSource = webConfig.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) configurationSource;
        CorsConfiguration config = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));

        assertEquals(properties.getAllowedOriginPatterns(), config.getAllowedOriginPatterns());
        assertEquals(properties.getAllowedMethods(), config.getAllowedMethods());
        assertEquals(properties.getAllowedHeaders(), config.getAllowedHeaders());
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
        assertEquals(7200L, config.getMaxAge());
    }
}
