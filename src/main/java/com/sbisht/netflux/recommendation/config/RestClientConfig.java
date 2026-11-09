package com.sbisht.netflux.recommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private final RestClient.Builder builder;
    private final RemoteServiceProperties properties;

    public RestClientConfig(RestClient.Builder builder, RemoteServiceProperties properties) {
        this.builder = builder;
        this.properties = properties;
    }

    @Bean
    public RestClient movieServiceRestClient() {
        return builder.clone()
                .baseUrl(properties.movieService().baseUrl())
                .build();
    }

    @Bean
    public RestClient userServiceRestClient() {
        return builder.clone()
                .baseUrl(properties.userService().baseUrl())
                .build();
    }
}
