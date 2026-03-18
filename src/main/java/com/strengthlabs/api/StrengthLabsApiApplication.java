package com.strengthlabs.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.strengthlabs")
public class StrengthLabsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StrengthLabsApiApplication.class, args);
    }
}
