package com.bankingsystem.repository;

import com.bankingsystem.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // New methods for uniqueness check excluding the current user
    Optional<User> findByUsernameAndIdNot(String username, String excludedUserId);

    Optional<User> findByEmailAndIdNot(String email, String excludedUserId);
}
