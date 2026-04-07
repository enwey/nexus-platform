package com.nexus.platform.config;

import com.nexus.platform.security.ApiAccessDeniedHandler;
import com.nexus.platform.security.ApiAuthenticationEntryPoint;
import com.nexus.platform.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/user/register",
                                "/user/login",
                                "/user/refresh",
                                "/user/send-code",
                                "/user/password/reset"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/discover/**",
                                "/game/public/list",
                                "/game/categories",
                                "/game/check-update",
                                "/game/list",
                                "/game/list/page",
                                "/game/download-url/*",
                                "/game/download/*",
                                "/game/*"
                        ).permitAll()
                        .requestMatchers("/social/**").permitAll()
                        .requestMatchers(
                                "/user/logout",
                                "/user/password/change",
                                "/user/devices/**",
                                "/user/logout-all",
                                "/user/terminate",
                                "/user/me",
                                "/user/profile",
                                "/wallet/**",
                                "/referral/**",
                                "/library/**",
                                "/game/upload",
                                "/game/submit/**",
                                "/game/*/submit-version/*",
                                "/game/*/rollback/*",
                                "/game/*/versions",
                                "/game/*/metadata",
                                "/game/developer/**",
                                "/game/approve/**",
                                "/game/reject/**",
                                "/audit/**",
                                "/admin/android/**",
                                "/admin/ops/**"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
