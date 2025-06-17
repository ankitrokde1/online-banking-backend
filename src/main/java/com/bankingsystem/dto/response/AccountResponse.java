package com.bankingsystem.dto.response;

import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private String id;
    private String accountNumber;      // Masked in getter
    private AccountType accountType;
    private BigDecimal balance;
    private boolean isActive;
    private LocalDateTime openAt;
    private String maskedAccountNumber;


    public static AccountResponse fromAccount(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .isActive(account.isActive())
                .openAt(account.getOpenedAt())
                .maskedAccountNumber(mask(account.getAccountNumber()))
                .build();
    }

    // Custom Getter to mask the account number (e.g., **** **** **** 1234)
    private static String mask(String number) {
        if (number == null || number.length() < 4) return "****";
        return "**** **** **** " + number.substring(number.length() - 4);
    }



}
