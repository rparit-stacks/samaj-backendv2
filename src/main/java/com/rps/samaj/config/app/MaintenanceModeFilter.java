package com.rps.samaj.config.app;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MaintenanceModeFilter implements Filter {

    private final RuntimeConfigService runtimeConfig;

    public MaintenanceModeFilter(RuntimeConfigService runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Allow admin paths and setup
        if (path.startsWith("/admin") || path.startsWith("/auth/setup") || path.startsWith("/auth/login")) {
            chain.doFilter(request, response);
            return;
        }

        // Check if maintenance mode is enabled
        if (runtimeConfig.isMaintenanceModeEnabled()) {
            String message = runtimeConfig.getMaintenanceMessage();

            httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");

            String jsonResponse = String.format(
                "{\"error\": \"Service Unavailable\", \"message\": \"%s\"}",
                escapeJson(message)
            );

            httpResponse.getWriter().write(jsonResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
