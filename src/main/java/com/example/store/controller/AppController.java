package com.example.store.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.LoginRequest;
import com.example.store.dto.LoginResponse;
import com.example.store.dto.RegisterRequest;
import com.example.store.service.AutherService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/")
public class AppController {

    @Autowired
    private AutherService autherService; 
    
    @GetMapping
    public String getMethodName() {
        return new String("Hellmamao");
    }

    //login API
    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        
        LoginResponse loginResponse = autherService.doLogin(loginRequest);
        if(loginResponse != null){
            System.out.println("Login Successful for user: " + loginResponse.getUsername());
            return ResponseEntity.ok(loginResponse);
        }
        return ResponseEntity.status(401).build();
    }


    //register API
    @PostMapping("register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        LoginResponse loginResponse = autherService.register(registerRequest);
        
        if(loginResponse != null){
            return ResponseEntity.created(null).body(loginResponse);
        }else{
            throw new RuntimeException("Registration failed");
        }
    }

    @DeleteMapping("user/{userName}")
    @PreAuthorize("hasRole('ADMIN') or #userName == authentication.principal.username")
    public ResponseEntity<Void> deleteUser(@PathVariable String userName){
        autherService.deleteUser(userName);
        return ResponseEntity.noContent().build();
    }
}
