package com.bankingsystem.entity;

import com.bankingsystem.entity.enums.TransactionStatus;
import com.bankingsystem.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    private String id;

    private TransactionType type; // DEPOSIT, WITHDRAW, TRANSFER
    private BigDecimal amount;

    private String sourceAccountId; // null for DEPOSIT
    private String targetAccountId; // null for WITHDRAW

    private LocalDateTime timestamp;
    private TransactionStatus status; // SUCCESS, FAILED, etc.
    private String description;
}
