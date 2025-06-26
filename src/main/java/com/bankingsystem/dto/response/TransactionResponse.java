package com.bankingsystem.dto.response;

import com.bankingsystem.entity.enums.TransactionStatus;
import com.bankingsystem.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String id;
    private String fromAccountNumber;
    private String toAccountNumber; // null for deposit/withdraw
    private BigDecimal amount;
    private TransactionType type; // "DEPOSIT", "WITHDRAW", "TRANSFER"
    private TransactionStatus status;
    private String description;
    private LocalDateTime timestamp;
}

