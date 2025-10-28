package com.example.store.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.*;

import com.example.store.exception.LoginException;
import com.example.store.repository.UserRepository;

@Service
public class UserDetailConfig  implements UserDetailsService{
    
    @Autowired
    private UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws LoginException {
        com.example.store.model.User user ;
        if(username.contains("@")) {
            user = userRepo.findByEmail(username);
        }else
            user = userRepo.findByUsername(username);
        
        if(user == null){
            throw new LoginException("Wrong username or password");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }

}
