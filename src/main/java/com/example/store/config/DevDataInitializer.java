package com.example.store.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.store.model.User;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;
import com.example.store.model.Role;

import jakarta.transaction.Transactional;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired 
    PasswordEncoder passwordEncoder;

    
    @Override
    @Transactional
    public void run(String... args){
        //addroles
        roleRepository.save(new Role("USER"));
        roleRepository.save(new Role("ADMIN"));

        //add users
        userRepository.save(new User("nhanhoa",passwordEncoder.encode("123456"),"hoa@example.com",2));
        userRepository.save(new User("admin",passwordEncoder.encode("123456"),"admin@example.com",1));
    }
}