package com.rps.samaj.config;

import com.rps.samaj.admin.system.web.AdminServiceAuthorizationFilter;
import com.rps.samaj.security.JwtAuthenticationFilter;
import com.rps.samaj.security.PermissiveAuthBridgeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            PermissiveAuthBridgeFilter permissiveAuthBridgeFilter,
            AdminServiceAuthorizationFilter adminServiceAuthorizationFilter,
            JsonAuthFailureHandlers jsonAuthFailureHandlers
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jsonAuthFailureHandlers)
                        .accessDeniedHandler(jsonAuthFailureHandlers))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers("/auth/register", "/auth/login", "/auth/login/otp", "/auth/refresh").permitAll()
                        .requestMatchers("/auth/setup/status", "/auth/setup").permitAll()
                        .requestMatchers("/auth/otp/**").permitAll()
                        .requestMatchers("/files/**").permitAll()
                        .requestMatchers("/api/v1/users/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/p/*/profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/p/*/contact").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/p/*/visible-profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/visible-profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/contact").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/matrimony/webhooks/chat").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "MODERATOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(permissiveAuthBridgeFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(adminServiceAuthorizationFilter, PermissiveAuthBridgeFilter.class);
        return http.build();
    }
}
