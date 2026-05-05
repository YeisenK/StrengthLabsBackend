package com.strengthlabs.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.strengthlabs")
@EnableJpaRepositories(basePackages = "com.strengthlabs.infrastructure.persistence.jpa")
@EntityScan(basePackages = "com.strengthlabs.infrastructure.persistence.jpa")
public class StrengthLabsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StrengthLabsApiApplication.class, args);
    }
}
