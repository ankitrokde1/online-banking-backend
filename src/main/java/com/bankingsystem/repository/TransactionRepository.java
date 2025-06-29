package com.bankingsystem.repository;

import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.enums.TransactionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

//    List<Transaction> findBySourceAccountIdOrTargetAccountId(String sourceAccountNumber, String targetAccountNumber);
    List<Transaction> findBySourceAccountNumberOrTargetAccountNumber(String sourceAccountNumber, String targetAccountNumber);
    List<Transaction> findByStatus(TransactionStatus status);

}
