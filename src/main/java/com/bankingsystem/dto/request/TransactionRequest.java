package com.bankingsystem.dto.request;

import com.bankingsystem.entity.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    private TransactionType type; // DEPOSIT, WITHDRAW, TRANSFER
//    private String sourceAccountId;
//    private String targetAccountId;

    private String sourceAccountNumber;
    private String targetAccountNumber;

    private BigDecimal amount;
    private String description;
}

