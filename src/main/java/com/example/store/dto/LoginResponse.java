package com.example.store.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String token;
    
    public LoginResponse(String username, String email, String role, String token) {
        this.username = username;
        this.role = role;
        this.token = token;
    }

}
