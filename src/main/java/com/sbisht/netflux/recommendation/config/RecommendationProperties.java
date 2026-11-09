package com.sbisht.netflux.recommendation.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.recommendation")
public record RecommendationProperties(List<String> genres,
                                       Integer sectionLimit) {
}
