package com.bankingsystem.repository;

import com.bankingsystem.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends MongoRepository<Account, String> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserId(String userId);

    boolean existsByAccountNumber(String accountNumber);

}
