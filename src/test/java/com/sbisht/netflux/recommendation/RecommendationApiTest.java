package com.sbisht.netflux.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.sbisht.netflux.recommendation.dto.HomeResponse;
import com.sbisht.netflux.recommendation.dto.MovieSummary;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.movie-service.base-url=http://movie-service",
                "app.user-service.base-url=http://user-service",
                "app.recommendation.genres=Action,Science Fiction",
                "app.recommendation.section-limit=20"
        }
)
@AutoConfigureRestTestClient
@AutoConfigureMockRestServiceServer
class RecommendationApiTest {

    private static final String MOVIE_1 = """
            {"id": 1, "title": "Movie One", "posterPath": "/p1.jpg", "backdropPath": "/b1.jpg",
             "voteAverage": 8.5, "releaseDate": "2024-01-15", "genres": ["Action"]}
            """;
    private static final String MOVIE_2 = """
            {"id": 2, "title": "Movie Two", "posterPath": "/p2.jpg", "backdropPath": "/b2.jpg",
             "voteAverage": 7.1, "releaseDate": "2023-06-01", "genres": ["Science Fiction"]}
            """;
    private static final String MOVIE_3 = """
            {"id": 3, "title": "Movie Three", "posterPath": "/p3.jpg", "backdropPath": "/b3.jpg",
             "voteAverage": 6.4, "releaseDate": "2025-03-20", "genres": ["Action", "Science Fiction"]}
            """;

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    MockRestServiceServer mockServer;

    @Test
    @DisplayName("Should return home recommendations aggregated from upstream services")
    void shouldReturnHomeRecommendations() {
        expectWatchlist("[1, 2]");
        expectMovies("http://movie-service/api/movies/batch?ids=1,2", "[" + MOVIE_1 + "," + MOVIE_2 + "]");
        expectMovies("http://movie-service/api/movies/popular?limit=20", "[" + MOVIE_3 + "]");
        expectMovies("http://movie-service/api/movies/latest?limit=20", "[" + MOVIE_2 + "," + MOVIE_3 + "]");
        expectMovies("http://movie-service/api/movies/search?genre=Action&limit=20", "[" + MOVIE_1 + "," + MOVIE_3 + "]");
        expectMovies("http://movie-service/api/movies/search?genre=Science%20Fiction&limit=20", "[" + MOVIE_2 + "]");

        var response = getHome("42");

        mockServer.verify();
        assertThat(response.watchlist())
                .extracting(MovieSummary::id)
                .containsExactly(1L, 2L);
        assertThat(response.popular())
                .extracting(MovieSummary::id)
                .containsExactly(3L);
        assertThat(response.latest())
                .extracting(MovieSummary::id)
                .containsExactly(2L, 3L);
        assertThat(response.byGenre().keySet()).containsExactly("Action", "Science Fiction");
        assertThat(response.byGenre().get("Action"))
                .extracting(MovieSummary::id)
                .containsExactly(1L, 3L);
        assertThat(response.byGenre().get("Science Fiction"))
                .extracting(MovieSummary::id)
                .containsExactly(2L);

        var movie = response.watchlist().getFirst();
        assertThat(movie.title()).isEqualTo("Movie One");
        assertThat(movie.posterPath()).isEqualTo("/p1.jpg");
        assertThat(movie.backdropPath()).isEqualTo("/b1.jpg");
        assertThat(movie.voteAverage()).isEqualTo(8.5);
        assertThat(movie.releaseDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(movie.genres()).containsExactly("Action");
    }

    @Test
    @DisplayName("Should include genre with empty list when search returns no movies")
    void shouldIncludeEmptyGenre() {
        expectWatchlist("[1]");
        expectMovies("http://movie-service/api/movies/batch?ids=1", "[" + MOVIE_1 + "]");
        expectMovies("http://movie-service/api/movies/popular?limit=20", "[" + MOVIE_1 + "]");
        expectMovies("http://movie-service/api/movies/latest?limit=20", "[" + MOVIE_1 + "]");
        expectMovies("http://movie-service/api/movies/search?genre=Action&limit=20", "[" + MOVIE_1 + "]");
        expectMovies("http://movie-service/api/movies/search?genre=Science%20Fiction&limit=20", "[]");

        var response = getHome("42");

        mockServer.verify();
        assertThat(response.byGenre().keySet()).containsExactly("Action", "Science Fiction");
        assertThat(response.byGenre().get("Science Fiction")).isEmpty();
    }

    @Test
    @DisplayName("Should skip batch call and return empty watchlist when watchlist is empty")
    void shouldSkipBatchWhenWatchlistEmpty() {
        expectWatchlist("[]");
        expectMovies("http://movie-service/api/movies/popular?limit=20", "[" + MOVIE_1 + "]");
        expectMovies("http://movie-service/api/movies/latest?limit=20", "[" + MOVIE_2 + "]");
        expectMovies("http://movie-service/api/movies/search?genre=Action&limit=20", "[" + MOVIE_1 + "]");
        expectMovies("http://movie-service/api/movies/search?genre=Science%20Fiction&limit=20", "[" + MOVIE_2 + "]");

        var response = getHome("42");

        mockServer.verify();
        assertThat(response.watchlist()).isEmpty();
        assertThat(response.popular()).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty section when upstream returns empty body")
    void shouldReturnEmptySectionForEmptyBody() {
        mockServer.expect(requestTo("http://user-service/api/users/watchlist"))
                .andRespond(withSuccess());
        mockServer.expect(requestTo("http://movie-service/api/movies/popular?limit=20"))
                .andRespond(withSuccess());
        expectMovies("http://movie-service/api/movies/latest?limit=20", "[" + MOVIE_2 + "]");
        expectMovies("http://movie-service/api/movies/search?genre=Action&limit=20", "[" + MOVIE_1 + "]");
        expectMovies("http://movie-service/api/movies/search?genre=Science%20Fiction&limit=20", "[" + MOVIE_2 + "]");

        var response = getHome("42");

        mockServer.verify();
        assertThat(response.watchlist()).isEmpty();
        assertThat(response.popular()).isEmpty();
        assertThat(response.latest()).hasSize(1);
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when X-User-Id header is missing")
    void shouldReturn401WhenUserIdMissing() {
        var problem = restTestClient.get()
                .uri("/api/recommendations/home")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        mockServer.verify();
        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getTitle()).isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when X-User-Id is not numeric")
    void shouldReturn400WhenUserIdNotNumeric() {
        var problem = getHomeExpectingBadRequest("abc");

        mockServer.verify();
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when X-User-Id is not positive")
    void shouldReturn400WhenUserIdNotPositive() {
        var problem = getHomeExpectingBadRequest("0");

        mockServer.verify();
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Should return 500 when upstream service fails")
    void shouldReturn500WhenUpstreamFails() {
        mockServer.expect(requestTo("http://user-service/api/users/watchlist"))
                .andRespond(withServerError());

        restTestClient.get()
                .uri("/api/recommendations/home")
                .header("X-User-Id", "42")
                .exchange()
                .expectStatus().isEqualTo(500);

        mockServer.verify();
    }

    private void expectWatchlist(String body) {
        mockServer.expect(requestTo("http://user-service/api/users/watchlist"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", "42"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectMovies(String url, String body) {
        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private HomeResponse getHome(String userId) {
        return restTestClient.get()
                .uri("/api/recommendations/home")
                .header("X-User-Id", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(HomeResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ProblemDetail getHomeExpectingBadRequest(String userId) {
        return restTestClient.get()
                .uri("/api/recommendations/home")
                .header("X-User-Id", userId)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();
    }
}
