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
            Authentication authentication) {
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
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<AccountResponse> getByAccountNumber(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccessDeniedException, AccountNotFoundException {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Account account;
        if (isAdmin) {
            // Admin can access any account
            account = accountService.getAccountByNumber(accountNumber)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountNumber));
        } else {
            // Regular users can only access their own accounts
            account = accountService.getAccountByIdAndUsername(accountNumber, username);
        }
        AccountResponse response = accountService.mapToResponse(account);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PutMapping("/deactivate/{accountNumber}")
    public ResponseEntity<String> deactivateAccount(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccountNotFoundException, AccessDeniedException {
        User user = (User) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Account account = accountService.getAccountByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountNumber));

        // Only admin or account owner can deactivate
        if (!isAdmin && !account.getUserId().equals(user.getId())) {
            throw new AccessDeniedException("You can only deactivate your own account.");
        }

        boolean changed = accountService.deactivateAccount(accountNumber);
        return changed
                ? ResponseEntity.ok("Account deactivated successfully.")
                : ResponseEntity.ok("Account was already deactivated.");
    }


    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PutMapping("/activate/{accountNumber}")
    public ResponseEntity<String> activateAccount(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccountNotFoundException, AccessDeniedException {
        User user = (User) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Account account = accountService.getAccountByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountNumber));

        // Only admin or account owner can activate
        if (!isAdmin && !account.getUserId().equals(user.getId())) {
            throw new AccessDeniedException("You can only activate your own account.");
        }

        boolean changed = accountService.activateAccount(accountNumber);
        return changed
                ? ResponseEntity.ok("Account activated successfully.")
                : ResponseEntity.ok("Account was already active.");
    }

}

