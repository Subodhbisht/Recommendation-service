package com.sbisht.netflux.recommendation.client;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.sbisht.netflux.recommendation.dto.MovieSummary;

@Component
public class MovieServiceClient {

    private static final ParameterizedTypeReference<List<MovieSummary>> MOVIE_LIST = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public MovieServiceClient(RestClient movieServiceRestClient) {
        this.restClient = movieServiceRestClient;
    }

    public List<MovieSummary> getPopular(Integer limit) {
        return fetch("/api/movies/popular?limit={limit}", limit);
    }

    public List<MovieSummary> getLatest(Integer limit) {
        return fetch("/api/movies/latest?limit={limit}", limit);
    }

    public List<MovieSummary> searchByGenre(String genre, Integer limit) {
        return fetch("/api/movies/search?genre={genre}&limit={limit}", genre, limit);
    }

    public List<MovieSummary> getByIds(List<Long> ids) {
        return fetch(uriBuilder -> uriBuilder
                .path("/api/movies/batch")
                .queryParam("ids", ids)
                .build());
    }

    private List<MovieSummary> fetch(String uriTemplate, Object... uriVariables) {
        var movies = restClient.get()
                .uri(uriTemplate, uriVariables)
                .retrieve()
                .body(MOVIE_LIST);
        return Objects.requireNonNullElse(movies, List.of());
    }

    private List<MovieSummary> fetch(Function<UriBuilder, URI> uriFunction) {
        var movies = restClient.get()
                .uri(uriFunction)
                .retrieve()
                .body(MOVIE_LIST);
        return Objects.requireNonNullElse(movies, List.of());
    }
}
