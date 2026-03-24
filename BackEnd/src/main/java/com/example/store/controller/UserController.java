package com.example.store.controller;

import org.springframework.web.bind.annotation.*;

import com.example.store.dto.UserChangePasswordRequest;
import com.example.store.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * Change password for current user
     */
    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody UserChangePasswordRequest request, Authentication authentication) {
        String username = authentication.getName();
        userService.changePassword(username, request);
    }
}
