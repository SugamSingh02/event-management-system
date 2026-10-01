package com.eventmanagment.repository;

import com.eventmanagment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndRole(String email, String role);

    @Query("SELECT u FROM User u WHERE LOWER(u.name) = LOWER(:identifier) OR LOWER(u.email) = LOWER(:identifier)")
    Optional<User> findByNameOrEmail(@Param("identifier") String identifier);

    @Query("""
            SELECT u.id
            FROM User u
            WHERE LOWER(u.name) = LOWER(:identifier)
            OR LOWER(u.email) = LOWER(:identifier)
            """)
    Optional<Long> findIdByNameOrEmail(@Param("identifier") String identifier);
}

