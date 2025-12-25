package com.example.store.dto;

/**
 * Response DTO for file upload
 */
public class MediaUploadResponse {
    
    private String url;
    private String message;
    
    public MediaUploadResponse() {
    }
    
    public MediaUploadResponse(String url, String message) {
        this.url = url;
        this.message = message;
    }
    
    // Getters and Setters
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
