package com.sbisht.netflux.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record RemoteServiceProperties(RemoteService movieService,
                                      RemoteService userService) {

    public record RemoteService(String baseUrl) {
    }
}
