package com.example.student_portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
// Configuration class to define RestTemplate bean for inter-service communication
public class RestTemplateConfig {

    @Bean
    // Provides RestTemplate instance used to call external microservices (e.g., Finance, Library)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}