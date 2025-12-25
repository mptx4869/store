package com.example.store.dto;

/**
 * Response DTO for file deletion
 */
public class MediaDeleteResponse {
    
    private String message;
    
    public MediaDeleteResponse() {
    }
    
    public MediaDeleteResponse(String message) {
        this.message = message;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
