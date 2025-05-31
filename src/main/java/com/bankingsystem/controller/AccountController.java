package com.bankingsystem.controller;

import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.User;
import com.bankingsystem.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // Create a new account (User or Admin)
    @PostMapping("/create")
    public ResponseEntity<Account> createAccount(
            @RequestParam String accountType,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        // String username = user.getUsername();
        Account created = accountService.createAccount(userId, accountType);
        return ResponseEntity.ok(created);
    }

    // Get all accounts for authenticated user
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<Account>> getUserAccounts(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AccountResponse> getByAccountNumber(@PathVariable String accountNumber, Authentication authentication) throws AccessDeniedException, AccountNotFoundException {
        String username = authentication.getName();
        Account account = accountService.getAccountByIdAndUsername(accountNumber, username);
        AccountResponse response = accountService.mapToResponse(account);
        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/deactivate/{accountNumber}")
    public ResponseEntity<String> deactivateAccount(@PathVariable String accountNumber) throws AccountNotFoundException {
        boolean changed = accountService.deactivateAccount(accountNumber);
        return changed
                ? ResponseEntity.ok("Account deactivated successfully.")
                : ResponseEntity.ok("Account was already deactivated.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/activate/{accountNumber}")
    public ResponseEntity<String> activateAccount(@PathVariable String accountNumber) throws AccountNotFoundException {
        boolean changed = accountService.activateAccount(accountNumber);
        return changed
                ? ResponseEntity.ok("Account activated successfully.")
                : ResponseEntity.ok("Account was already active.");
    }

}

