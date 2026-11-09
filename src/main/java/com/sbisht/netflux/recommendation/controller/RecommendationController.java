package com.sbisht.netflux.recommendation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sbisht.netflux.recommendation.dto.HomeResponse;
import com.sbisht.netflux.recommendation.service.RecommendationService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/home")
    public HomeResponse getHome(@RequestHeader("X-User-Id") @Positive Long userId) {
        return recommendationService.getHome(userId);
    }
}
