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
                        // Public CMS banners (used on login/home shells)
                        .requestMatchers(HttpMethod.GET, "/api/banners/**").permitAll()
                        // ----------------------------
                        // Authenticated user endpoints
                        // ----------------------------
                        .requestMatchers(HttpMethod.GET, "/api/v1/emergencies").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/emergencies/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/emergencies/*/helpers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/emergencies/helpers/*/stats").permitAll()
                        .requestMatchers("/api/v1/emergencies/me").authenticated()
                        .requestMatchers("/api/v1/emergencies/dashboard").authenticated()

                        .requestMatchers("/api/v1/directory/me/**").authenticated()
                        .requestMatchers("/api/v1/documents/me").authenticated()
                        .requestMatchers("/api/v1/gallery/albums/me").authenticated()
                        .requestMatchers("/api/v1/community/me/**").authenticated()

                        .requestMatchers("/api/v1/notifications/**").authenticated()
                        .requestMatchers("/api/v1/device-tokens/**").authenticated()
                        .requestMatchers("/api/v1/chat/**").authenticated()
                        .requestMatchers("/api/v1/matrimony/**").authenticated()
                        .requestMatchers("/api/v1/exams/**").authenticated()
                        .requestMatchers("/api/v1/suggestions/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/donations/config").permitAll()
                        .requestMatchers("/api/v1/donations/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/business").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/business/*").permitAll()
                        .requestMatchers("/api/v1/business/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/*").permitAll()
                        .requestMatchers("/api/v1/jobs/**").authenticated()

                        .requestMatchers("/auth/register", "/auth/login", "/auth/login/otp", "/auth/refresh").permitAll()
                        .requestMatchers("/auth/setup/status", "/auth/setup").permitAll()
                        .requestMatchers("/auth/otp/**").permitAll()
                        .requestMatchers("/auth/google/**").permitAll()
                        .requestMatchers("/auth/admin-invite/**").permitAll()
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
                        // ----------------------------
                        // Public read-only app content
                        // ----------------------------
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/directory").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/directory/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/history/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/gallery/albums").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/gallery/albums/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/*/file").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/tags").permitAll()
                        // tracking endpoints that don't require auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/posts/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/posts/*/share").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/emergencies/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/emergencies/*/contact-click").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(permissiveAuthBridgeFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(adminServiceAuthorizationFilter, PermissiveAuthBridgeFilter.class);
        return http.build();
    }
}
