package com.example.store.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex) {
        
        System.err.println("An error occurred: " + ex.getMessage());
        return ResponseEntity.internalServerError().body(Map.of("error","An internal server error occurred."));
    }

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<?> handleLoginException(LoginException ex) {
        
        System.err.println("Login error: " + ex.getMessage());
        return ResponseEntity.status(401).body(ex.getMessage());
    } 

    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        
        System.err.println("Validation error: " + ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler (ConflictException.class)
    public ResponseEntity<?> handleConflictException(ConflictException ex) {
        
        System.err.println("Conflict error: " + ex.getMessage());
        return ResponseEntity.status(409).body(ex.getMessage());
    }
}
