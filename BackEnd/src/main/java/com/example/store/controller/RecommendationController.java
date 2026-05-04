package com.example.store.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.example.store.service.RecommendationService;
import com.example.store.dto.BookListResponse;
import java.util.List;

@RestController
@RequestMapping("/recommendation")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<List<BookListResponse>> getRecommendations(
            @PathVariable String username,
            @RequestParam(defaultValue = "10") int k) {
        List<BookListResponse> result = recommendationService.getRecommendations(username, k);
        System.out.println("[REC-CTRL] Sending " + result.size() + " books to frontend for user=" + username);
        if (!result.isEmpty()) {
            System.out.println("[REC-CTRL] Sample: " + result.get(0).getId() + " / " + result.get(0).getTitle());
        }
        return ResponseEntity.ok(result);
    }
}
