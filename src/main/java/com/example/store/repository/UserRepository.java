package com.example.store.repository;


import com.example.store.model.User;
import org.springframework.data.repository.Repository;


public interface UserRepository extends Repository<User, Long> {
    
    User findByUsername(String username);
    
    
}
 