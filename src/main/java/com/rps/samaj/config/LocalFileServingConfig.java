package com.rps.samaj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class LocalFileServingConfig implements WebMvcConfigurer {

    private final SamajProperties properties;

    public LocalFileServingConfig(SamajProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Path.of(properties.getStorage().getRoot()).toAbsolutePath().normalize();
        String location = "file:" + root + java.io.File.separator;
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }
}
