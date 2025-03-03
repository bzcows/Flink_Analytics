package com.example.ccaggregator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI ccAggregatorOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("CC Aggregator API")
                        .description("API documentation for CC Aggregator application")
                        .version("v1.0"));
    }
}