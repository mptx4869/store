package com.example.store.repository;

import com.example.store.model.Role;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface RoleRepository extends Repository<Role, Long> {
    Role findById(long id);
    Role findByName(String name);
    List<Role> findAll();
    void save(Role role);
    void delete(Role role);

}
