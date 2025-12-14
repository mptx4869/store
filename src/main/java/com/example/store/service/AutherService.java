package com.example.store.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.store.config.JwtConfig;
import com.example.store.dto.LoginRequest;
import com.example.store.dto.LoginResponse;
import com.example.store.dto.RegisterRequest;
import com.example.store.exception.ConflictException;
import com.example.store.exception.LoginException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

import jakarta.transaction.Transactional;

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

    @Autowired
    private RoleRepository roleRepository;

    public LoginResponse doLogin( LoginRequest loginRequest){
        
        System.out.println("Attempting login for user: " + loginRequest.getUsername() + "pass:" + loginRequest.getPassword());

       
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
                jwtConfig.generateToken(principal)
            );

        }catch(Exception ex){
            System.out.println("Login failed for user: " + loginRequest.getUsername() + " - " + ex.getMessage());
            throw new LoginException(ex.getMessage());
        }
    }

    //register method
    @Transactional
    public LoginResponse register(RegisterRequest registerRequest){

        if(userRepo.existsByUsername(registerRequest.getUsername())){
            throw new ConflictException("Username already exists");
        }

        if(userRepo.existsByEmail(registerRequest.getEmail())){
            throw new ConflictException("Email already exists");
        }

        Long roleId = roleRepository.getIdByName(registerRequest.getRoleName())
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        Role role = roleRepository.getReferenceById(roleId);

        User newUser = new User(
            registerRequest.getUsername(),
            passwordEncoder.encode(registerRequest.getPassword()),
            registerRequest.getEmail(),
            "ACTIVE",
            role
        );
        userRepo.save(newUser);

        return doLogin(new LoginRequest(registerRequest.getUsername(),registerRequest.getPassword()));

    }  

    @Transactional
    public void deleteUser(String userName){
        User user = userRepo.findByUsername(userName)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Authentication auth = 
            SecurityContextHolder
            .getContext()
            .getAuthentication();

        if (auth.getName().equals(userName)) {
            userRepo.delete(user);
        }else{
            throw new LoginException("Unauthorized to delete this user");
        }
        
    }
}
