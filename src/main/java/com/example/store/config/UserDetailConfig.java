package com.example.store.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.*;

import com.example.store.exception.LoginException;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

@Service
public class UserDetailConfig  implements UserDetailsService{
    
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws LoginException {
        com.example.store.model.User user = username.contains("@")
            ? userRepo.findByEmail(username).orElse(null)
            : userRepo.findByUsername(username).orElse(null);

        if (user == null) {
            throw new LoginException("Wrong username");
        }

        if( !user.getStatus().equals("ACTIVE")){
            throw new LoginException("User is not active");
        }
            
        String roleName = user.getRole() != null
            ? user.getRole().getName()
            : roleRepo.findByName("CUSTOMER").map(com.example.store.model.Role::getName).orElse("CUSTOMER");

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(roleName)
                .build();
    }

}
