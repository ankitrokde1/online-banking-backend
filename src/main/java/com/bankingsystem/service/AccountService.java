package com.bankingsystem.service;

import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.enums.AccountType;
import com.bankingsystem.exception.UserNotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.util.AccountNumberGenerator;
import com.bankingsystem.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public Account createAccount(String userId, String accountType) {
        AccountType type;
        try {
            type = AccountType.valueOf(accountType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid account type '" + accountType + "'. Allowed types are: " +
                            java.util.Arrays.toString(AccountType.values()));
        }

        Account account = Account.builder()
                .userId(userId)
                .accountNumber(AccountNumberGenerator.generate())
                .balance(BigDecimal.ZERO)
                .accountType(type)
                .active(true)
                .openedAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }

   public List<Account> getAccountsByUserId(String userId) {
    // Check if user exists
    if (!userRepository.existsById(userId)) {
        throw new UserNotFoundException("User not found with id: " + userId);
    }
    // Return accounts (empty list if none)
    return accountRepository.findByUserId(userId);
}

    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public boolean deactivateAccount(String accountNumber) throws AccountNotFoundException {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> {
                    if (!account.isActive())
                        return false; // Already inactive
                    account.setActive(false);
                    accountRepository.save(account);
                    return true;
                })
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    public boolean activateAccount(String accountNumber) throws AccountNotFoundException {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> {
                    if (account.isActive())
                        return false; // Already active
                    account.setActive(true);
                    accountRepository.save(account);
                    return true;
                })
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    public AccountResponse mapToResponse(Account account) {
        return AccountResponse.fromAccount(account);
    }

    public Account getAccountByIdAndUsername(String accountNumber, String username)
            throws AccountNotFoundException, AccessDeniedException {
        // Find user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        // Find an account by id
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountNumber));

        // Verify that account belongs to user
        if (!account.getUserId().equals(user.getId())) {
            throw new AccessDeniedException("Account does not belong to the user");
        }

        return account;
    }
}

