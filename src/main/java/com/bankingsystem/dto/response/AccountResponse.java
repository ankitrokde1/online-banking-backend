package com.bankingsystem.dto.response;

import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private String id;
    private String accountNumber;      // Masked in getter
    private AccountType accountType;
    private BigDecimal balance;
    private boolean isActive;
    private LocalDateTime openAt;
    private String maskedAccountNumber;


    public static AccountResponse fromAccount(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.isActive(),
                account.getOpenedAt(),
                mask(account.getAccountNumber())
        );
    }

    // Custom Getter to mask the account number (e.g., **** **** **** 1234)
    private static String mask(String number) {
        if (number == null || number.length() < 4) return "****";
        return "**** **** **** " + number.substring(number.length() - 4);
    }


}
