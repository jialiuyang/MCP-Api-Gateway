package com.mcpg.web.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({SecurityProperties.class, SsrfProperties.class})
public class SecurityConfig {

    private final SecurityProperties properties;
    private final ApiTokenAuthenticationFilter tokenFilter;

    public SecurityConfig(SecurityProperties properties, ApiTokenAuthenticationFilter tokenFilter) {
        this.properties = properties;
        this.tokenFilter = tokenFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/system/auth-config").permitAll();
                    if (!properties.isEnabled()) {
                        auth.anyRequest().permitAll();
                    } else {
                        auth.requestMatchers(
                                        "/api/**",
                                        "/mcp",
                                        "/mcp/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/h2-console",
                                        "/h2-console/**")
                                .authenticated();
                        auth.anyRequest().permitAll();
                    }
                })
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
