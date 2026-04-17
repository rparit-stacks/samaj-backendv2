package com.rps.samaj;

import com.rps.samaj.config.SamajProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SamajProperties.class)
public class SamajApplication {

    public static void main(String[] args) {
        SpringApplication.run(SamajApplication.class, args);
    }
}
