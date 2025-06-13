package com.bankingsystem.repository;

import com.bankingsystem.entity.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
//    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(String userId);
    Optional<PasswordResetToken> findByUserId(String userId);
    List<PasswordResetToken> findByExpiryDateAfter(Date date);
}

