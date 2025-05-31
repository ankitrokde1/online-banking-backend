package com.bankingsystem.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    private String type; // DEPOSIT, WITHDRAW, TRANSFER
    private String sourceAccountId;
    private String targetAccountId;
    private BigDecimal amount;
    private String description;
}

