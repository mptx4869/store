package com.example.store.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Admin methods — legacy single-filter queries (kept for compatibility)
    @EntityGraph(attributePaths = "role")
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    @EntityGraph(attributePaths = "role")
    Page<User> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = "role")
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.status = :status")
    Page<User> findByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") String status, Pageable pageable);

    @Query("SELECT u.id FROM User u WHERE u.username = :username")
    Optional<Long> findIdByUsername(@Param("username") String username);

    /**
     * Admin user list: single JPQL query with optional filters.
     * Sort is driven by Pageable — Spring Data JPA appends ORDER BY directly,
     * allowing PostgreSQL to use indexes on individual columns instead of
     * filesort from the old CASE WHEN approach.
     * totalOrders is read from the denormalized u.totalOrders column (no JOIN to orders).
     */
    /**
     * usernamePattern must be pre-built as "%" + username.toLowerCase() + "%" or null.
     * Avoids CONCAT in JPQL which triggers the Hibernate 6 + PostgreSQL lower(bytea) bug
     * when the parameter is null (OID-0 type inference failure).
     */
    @Query(value = """
            SELECT u.id        AS id,
                   u.username  AS username,
                   u.email     AS email,
                   r.name      AS role,
                   u.status    AS status,
                   u.createdAt AS createdAt,
                   u.totalOrders AS totalOrders
            FROM User u JOIN u.role r
            WHERE (:roleName IS NULL OR r.name = :roleName)
              AND (:status   IS NULL OR u.status = :status)
              AND (:usernamePattern IS NULL OR LOWER(u.username) LIKE :usernamePattern)
            """,
            countQuery = """
            SELECT COUNT(u) FROM User u JOIN u.role r
            WHERE (:roleName IS NULL OR r.name = :roleName)
              AND (:status   IS NULL OR u.status = :status)
              AND (:usernamePattern IS NULL OR LOWER(u.username) LIKE :usernamePattern)
            """)
    Page<AdminUserListRow> findAdminUserList(
            @Param("roleName") String roleName,
            @Param("status") String status,
            @Param("usernamePattern") String usernamePattern,
            Pageable pageable
    );

    /**
     * Keyset next-page query — cursor params are ALWAYS non-null.
     *
     * <p>Root cause of the previous {@code lower(bytea)} bug: passing a null
     * {@code LocalDateTime} via {@code setObject(index, null)} gives PostgreSQL
     * OID 0 (unknown), which breaks type inference for every other {@code ?}
     * in the same query — including the String LIKE pattern — causing them to
     * be treated as {@code bytea}.
     *
     * <p>Fix: never pass a null {@code LocalDateTime} to this method.  The
     * caller handles the first page separately (no cursor params at all).
     *
     * <p>Sort is hardcoded as {@code ORDER BY u.createdAt DESC, u.id DESC}.
     * Pass an <em>unsorted</em> {@code Pageable} so Spring Data does not append
     * a conflicting ORDER BY.
     */
    @Query("""
            SELECT u.id          AS id,
                   u.username    AS username,
                   u.email       AS email,
                   r.name        AS role,
                   u.status      AS status,
                   u.createdAt   AS createdAt,
                   u.totalOrders AS totalOrders
            FROM User u JOIN u.role r
            WHERE (:roleName IS NULL OR r.name = :roleName)
              AND (:status   IS NULL OR u.status = :status)
              AND (:usernamePattern IS NULL OR LOWER(u.username) LIKE :usernamePattern)
              AND (u.createdAt < :lastCreatedAt
                   OR (u.createdAt = :lastCreatedAt AND u.id < :lastId))
            ORDER BY u.createdAt DESC, u.id DESC
            """)
    Slice<AdminUserListRow> findAdminUserListKeysetNext(
            @Param("roleName") String roleName,
            @Param("status") String status,
            @Param("usernamePattern") String usernamePattern,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    /**
     * Atomically increments total_orders for a user.
     * Called inside OrderService.createOrder transaction to keep the
     * denormalized counter in sync without a read-modify-write race.
     */
    @Modifying
    @Query("UPDATE User u SET u.totalOrders = u.totalOrders + 1 WHERE u.id = :userId")
    void incrementTotalOrders(@Param("userId") Long userId);

    interface AdminUserListRow {
        Long getId();
        String getUsername();
        String getEmail();
        String getRole();
        String getStatus();
        LocalDateTime getCreatedAt();
        Long getTotalOrders();
    }
}
