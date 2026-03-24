package com.example.store.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String error;
    private String message;
    private String code;
    private String path;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
