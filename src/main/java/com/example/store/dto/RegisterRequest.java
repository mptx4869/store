package com.example.store.dto;

import lombok.*;

@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    String username;
    String password;
    String email;
    String role;
}
