package com.bankingsystem.repository;

import com.bankingsystem.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameAndIdNot(String username, String excludedUserId);

    Optional<User> findByEmailAndIdNot(String email, String excludedUserId);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String token);

}
