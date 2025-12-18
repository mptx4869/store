package com.example.store.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void deleteByUsername(String username);

    // Admin methods
    @EntityGraph(attributePaths = "role")
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    @EntityGraph(attributePaths = "role")
    Page<User> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = "role")
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.status = :status")
    Page<User> findByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") String status, Pageable pageable);
}