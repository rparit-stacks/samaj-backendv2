package com.rps.samaj;

import com.rps.samaj.config.SamajProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableConfigurationProperties(SamajProperties.class)
@EnableCaching
public class SamajApplication {

    public static void main(String[] args) {
        SpringApplication.run(SamajApplication.class, args);
    }
}
