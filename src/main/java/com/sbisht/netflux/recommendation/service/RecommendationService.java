package com.sbisht.netflux.recommendation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sbisht.netflux.recommendation.client.MovieServiceClient;
import com.sbisht.netflux.recommendation.client.UserServiceClient;
import com.sbisht.netflux.recommendation.config.RecommendationProperties;
import com.sbisht.netflux.recommendation.dto.HomeResponse;
import com.sbisht.netflux.recommendation.dto.MovieSummary;

@Service
public class RecommendationService {

    private final MovieServiceClient movieServiceClient;
    private final UserServiceClient userServiceClient;
    private final RecommendationProperties properties;

    public RecommendationService(MovieServiceClient movieServiceClient,
                                 UserServiceClient userServiceClient,
                                 RecommendationProperties properties) {
        this.movieServiceClient = movieServiceClient;
        this.userServiceClient = userServiceClient;
        this.properties = properties;
    }

    public HomeResponse getHome(Long userId) {
        var limit = properties.sectionLimit();
        return new HomeResponse(
                getWatchlist(userId),
                movieServiceClient.getPopular(limit),
                movieServiceClient.getLatest(limit),
                getByGenre(limit)
        );
    }

    private List<MovieSummary> getWatchlist(Long userId) {
        var ids = userServiceClient.getWatchlist(userId);
        return ids.isEmpty() ? List.of() : movieServiceClient.getByIds(ids);
    }

    private Map<String, List<MovieSummary>> getByGenre(Integer limit) {
        return properties.genres().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        genre -> movieServiceClient.searchByGenre(genre, limit),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }
}
