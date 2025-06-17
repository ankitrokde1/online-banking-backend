package com.bankingsystem.controller;

import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.User;
import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static com.bankingsystem.util.AuthUtils.isAdmin;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    // Create a new account (User or Admin)
    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(
            @RequestParam String accountType,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        AccountResponse created = accountService.createAccount(userId, accountType);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Get all accounts for authenticated user
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<AccountResponse>> getUserAccounts(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        List<AccountResponse> accountResponse = accounts.stream()
                .map(accountService::mapToResponse)
                .toList();
        return ResponseEntity.ok(accountResponse);
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<AccountResponse> getByAccountNumber(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccessDeniedException, AccountNotFoundException {
        User user = (User) authentication.getPrincipal();
        
        boolean isAdmin = isAdmin(authentication);

        Account account = accountService.getAuthorizedAccount(accountNumber, user, isAdmin);
       
        AccountResponse response = accountService.mapToResponse(account);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PutMapping("/deactivate/{accountNumber}")
    public ResponseEntity<?> deactivateAccount(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccountNotFoundException, AccessDeniedException {
        User user = (User) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);

        Account account = accountService.getAccountByNumber(accountNumber);
        accountService.verifyOwnershipOrAdmin(account, user, isAdmin);

        boolean changed = accountService.deactivateAccount(accountNumber);
        return changed
                ? ResponseEntity.ok(Map.of("message", "Account deactivated successfully."))
                : ResponseEntity.ok(Map.of("message", "Account was already deactivated."));
    }


    @PutMapping("/activate/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> activateAccount(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccountNotFoundException, AccessDeniedException {

        User user = (User) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);

        // ✅ Fetch an account without checking `active` status
        Account account = accountService.getAccountByNumber(accountNumber);

        // ✅ Check ownership/admin AFTER fetching
        accountService.verifyOwnershipOrAdmin(account, user, isAdmin);

        boolean changed = accountService.activateAccount(accountNumber);
        return changed
                ? ResponseEntity.ok(Map.of("message", "Account activated successfully."))
                : ResponseEntity.ok(Map.of("message", "Account was already active."));
    }

}

