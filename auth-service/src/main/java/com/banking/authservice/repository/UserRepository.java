package com.banking.authservice.repository;

import com.banking.authservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByKeycloakUserId(String keycloakUserId);

    User findByUserName(String userName);

    Optional<User> findByEmail(String email);


    @Query("""
        SELECT u
        FROM User u
        WHERE (
            :query IS NULL
            OR :query = ''
            OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        """)
    Page<User> searchUsers(
            @Param("query") String query,
            Pageable pageable
    );
}
