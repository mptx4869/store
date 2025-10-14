package com.example.store.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.store.repository.UserRepository;


import com.example.store.dto.LoginRequest;
import com.example.store.dto.LoginResponse;
import com.example.store.model.*;
import com.example.store.exception.*;

@Service
public class AutherService {
    
    @Autowired
    private UserRepository userRepo;

    public LoginResponse doLogin( LoginRequest loginRequest){
        //check username and password from database
        //if valid, generate token and return LoginResponse
        //if invalid, return null or throw exception
        
        System.out.println("Attempting login for user: " + loginRequest.getUsername());

        User user ;
        if(Validation.isValidEmail(loginRequest.getUsername())) {
            user = userRepo.findByEmail(loginRequest.getUsername());
        }else
            user = userRepo.findByUsername(loginRequest.getUsername());
        
        if(user == null){
            throw new LoginException("Wrong username or password");
        }

        if(user!= null && user.getPassword().equals(loginRequest.getPassword())){
            //generate token (dummy token for now)
            return new LoginResponse(user.getUserId(), user.getEmail(), user.getRole(), "dummy-token");
        }else{
            throw new LoginException("Wrong username or password");
        }
    }

    //register method
    public User register(User user){
        if(userRepo.existsByEmail(user.getEmail())){
            throw new ConflictException("Email already exists");
        }
        return userRepo.save(user);
    }  
}
