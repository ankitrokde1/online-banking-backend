package com.bankingsystem.repository;

import com.bankingsystem.entity.AccountRequest;
import com.bankingsystem.entity.enums.RequestStatus;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AccountRequestRepository extends MongoRepository<AccountRequest, String> {
    List<AccountRequest> findByStatus(RequestStatus status);
}
