package com.bankingsystem.dto.response;

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
    private String fromAccountId;
    private String toAccountId; // null for deposit/withdraw
    private BigDecimal amount;
    private String type; // "DEPOSIT", "WITHDRAW", "TRANSFER"
    private LocalDateTime timestamp;
}

