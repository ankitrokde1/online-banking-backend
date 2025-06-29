package com.bankingsystem.entity;

import com.bankingsystem.entity.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {


    @Id
    private String id;

    private String userId; // Reference to User
    @Indexed(unique = true)
    private String accountNumber;
    private BigDecimal balance;
    private AccountType accountType; // e.g., SAVINGS, CURRENT
    private boolean active;
    private LocalDateTime openedAt;

    @Builder.Default
    private String currency = "INR";
}

