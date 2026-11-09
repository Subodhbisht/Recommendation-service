package com.sbisht.netflux.recommendation.client;

import java.util.List;
import java.util.Objects;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(RestClient userServiceRestClient) {
        this.restClient = userServiceRestClient;
    }

    public List<Long> getWatchlist(Long userId) {
        var ids = restClient.get()
                .uri("/api/users/watchlist")
                .header("X-User-Id", String.valueOf(userId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Long>>() {});
        return Objects.requireNonNullElse(ids, List.of());
    }
}
