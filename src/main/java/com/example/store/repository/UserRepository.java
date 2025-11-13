package com.example.store.repository;


import com.example.store.model.User;
import org.springframework.data.repository.Repository;


public interface UserRepository extends Repository<User, Long> {
    
    User findByUsername(String username);
    User save(User user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User findByEmail(String email);
    User findByUserId(Long userId);
    void deleteById(Long userId);
    void deleteByUsername(String username);
    
}