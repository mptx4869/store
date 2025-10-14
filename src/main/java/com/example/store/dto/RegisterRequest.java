package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Username cannot be empty")
    String username;

    @NotBlank(message = "Password cannot be empty")
    String password;

    @NotBlank(message = "Email cannot be empty")
    String email;

    @NotBlank(message = "Role cannot be empty")
    String role;
}
