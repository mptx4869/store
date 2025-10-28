package com.example.store.config;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtConfig jwtConfig;
    private final SecurityConfig securityConfig;
    private final UserDetailConfig userDetailConfig;

    public JwtAuthenticationFilter(
        JwtConfig jwtConfig,
        SecurityConfig securityConfig,
        UserDetailConfig userDetailConfig
    ){
        this.jwtConfig = jwtConfig;
        this.securityConfig = securityConfig;
        this.userDetailConfig = userDetailConfig;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        return ;
        // Implementation for filtering JWT from requests will go here
    }
}
