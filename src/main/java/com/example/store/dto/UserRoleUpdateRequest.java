package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateRequest {
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(ADMIN|CUSTOMER|admin|customer)$", 
             message = "Role must be either ADMIN or CUSTOMER")
    private String role;
}
