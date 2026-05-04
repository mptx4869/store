package com.example.store.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.store.dto.BookListResponse;

import java.util.List;

@Service
public class RecommendationService {

    private final RestTemplate restTemplate;
    private final BookService bookService;

    @Value("${recommendation.api.url:http://localhost:8000/api/v1/recommend/user/}")
    private String recommendationApiUrl;

    public RecommendationService(RestTemplate restTemplate, BookService bookService) {
        this.restTemplate = restTemplate;
        this.bookService = bookService;
    }

    public List<BookListResponse> getRecommendations(String username, int k) {
        String url = recommendationApiUrl + username + "?k=" + k;
        try {
            String rawResponse = restTemplate.getForObject(url, String.class);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            PythonRecommendationResponse response = mapper.readValue(rawResponse, PythonRecommendationResponse.class);

            if (response != null && response.getBookIds() != null && !response.getBookIds().isEmpty()) {
                List<Long> bookIds = response.getBookIds().stream()
                        .map(Long::valueOf)
                        .toList();
                System.out.println("[REC-SVC] Book IDs from Python: " + bookIds);
                List<BookListResponse> books = bookService.getBooksByIdsOrdered(bookIds);
                System.out.println("[REC-SVC] Books fetched from DB: " + books.size());
                if (!books.isEmpty()) {
                    System.out.println("[REC-SVC] First book: id=" + books.get(0).getId() + ", title=" + books.get(0).getTitle());
                }
                return books;
            } else {
                System.out.println("[REC-SVC] Python response has no book_ids.");
            }
        } catch (Exception e) {
            System.err.println("[REC-SVC] Error: " + e.getMessage());
            e.printStackTrace();
        }
        return List.of();
    }
    
    static class PythonRecommendationResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("user_id")
        private String userId;
        
        @com.fasterxml.jackson.annotation.JsonProperty("k")
        private int k;
        
        @com.fasterxml.jackson.annotation.JsonProperty("n_profile_books")
        private int nProfileBooks;
        
        @com.fasterxml.jackson.annotation.JsonProperty("book_ids")
        private List<String> bookIds;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public int getK() { return k; }
        public void setK(int k) { this.k = k; }
        
        public int getNProfileBooks() { return nProfileBooks; }
        public void setNProfileBooks(int nProfileBooks) { this.nProfileBooks = nProfileBooks; }
        
        public List<String> getBookIds() { return bookIds; }
        public void setBookIds(List<String> bookIds) { this.bookIds = bookIds; }
    }
}
