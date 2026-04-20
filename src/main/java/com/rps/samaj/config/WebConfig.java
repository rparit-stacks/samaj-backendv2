package com.rps.samaj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS for browser clients (e.g. Vercel). Spring Security uses {@link CorsConfigurationSource}
 * via {@code http.cors(...)} — WebMvc-only {@code addCorsMappings} is not enough on its own.
 * <p>
 * Origin matching ignores URL path; trailing slashes on page URLs do not affect CORS.
 * {@code https://a.com} and {@code https://www.a.com} are different origins — list both in
 * {@code samaj.cors.additional-origin-patterns} if users hit your app on both hosts.
 */
@Configuration
public class WebConfig {

    private final SamajProperties samajProperties;

    public WebConfig(SamajProperties samajProperties) {
        this.samajProperties = samajProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> patterns = new ArrayList<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.1.8:*",
                "http://192.168.137.1:*",
                "https://*.vercel.app",
                "https://www.*.vercel.app",
                "https://frontend-flame-nine-37.vercel.app"
        ));
        SamajProperties.Cors extra = samajProperties.getCors();
        if (extra != null && extra.getAdditionalOriginPatterns() != null) {
            for (String p : extra.getAdditionalOriginPatterns()) {
                if (p != null && !p.isBlank()) {
                    patterns.add(p.trim());
                }
            }
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "X-Auth-Token"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
