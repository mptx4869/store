package com.example.store.service;

import java.net.PasswordAuthentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.store.repository.UserRepository;
import com.example.store.config.JwtConfig;
import com.example.store.dto.LoginRequest;
import com.example.store.dto.LoginResponse;
import com.example.store.dto.RegisterRequest;
import com.example.store.model.*;
import com.example.store.exception.*;

@Service
public class AutherService {
    
    @Autowired
    private UserRepository userRepo;
    
    @Autowired 
    AuthenticationManager authenManager;

    @Autowired 
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtConfig jwtConfig;

    public LoginResponse doLogin( LoginRequest loginRequest){
        
        System.out.println("Attempting login for user: " + loginRequest.getUsername());

       
        try{
            System.out.println("in try");
            
            UserDetails principal =(UserDetails) authenManager
                .authenticate(
                    new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(), 
                        loginRequest.getPassword()
                    )
                ).getPrincipal();
            System.out.println("login token: " + jwtConfig.generateToken(principal));
            System.out.println("Login successful for user: " + loginRequest.getUsername());

            return new LoginResponse(
                principal.getUsername(),
                "USER",
                "jwtConfig.generateToken(principal)"
            );

        }catch(Exception ex){
            System.out.println("Login failed for user: " + loginRequest.getUsername() + " - " + ex.getMessage());
            throw new LoginException("Login failed");
        }
    }

    //register method
    public LoginResponse register(RegisterRequest registerRequest){

        if(userRepo.existsByUsername(registerRequest.getUsername())){
            throw new ConflictException("Username already exists");
        }

        if(userRepo.existsByEmail(registerRequest.getEmail())){
            throw new ConflictException("Email already exists");
        }

        User newUser = new User(
            registerRequest.getUsername()
            ,passwordEncoder.encode(registerRequest.getPassword())
            ,registerRequest.getEmail()
            ,registerRequest.getRole_id()
        );
        userRepo.save(newUser);

        return doLogin(new LoginRequest(registerRequest.getUsername(),registerRequest.getPassword()));

    }  

    public void deleteUser(String userName){
        userRepo.deleteByUsername(userName);
    }
}
