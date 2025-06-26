package com.bankingsystem.controller;

import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.User;
import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    //done
    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(
            @RequestParam String accountType,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        logger.info("User [{}] is creating account of type [{}] (isAdmin={})", user.getUsername(), accountType, isAdmin);

        AccountResponse response = accountService.createAccount(user.getId(), accountType, isAdmin);
        logger.info("Account creation request processed for user [{}]", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //done
    // Get all accounts for authenticated user
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getUserAccounts(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        logger.info("Fetching accounts for user [{}]", user.getUsername());

        List<Account> accounts = accountService.getAccountsByUserId(userId);

//        if (accounts.isEmpty()) {
//            logger.warn("No accounts found for user [{}]", user.getUsername());
//            return ResponseEntity
//                    .status(HttpStatus.NOT_FOUND)
//                    .body(Map.of("message", "No accounts found for the user."));
//        }

        List<AccountResponse> accountResponse = accounts.stream()
                .map(accountService::mapToResponse)
                .toList();
        return ResponseEntity.ok(accountResponse);
    }

    //done
    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<AccountResponse> getByAccountNumber(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccessDeniedException, AccountNotFoundException {
        User user = (User) authentication.getPrincipal();

        boolean isAdmin = isAdmin(authentication);

        logger.info("User [{}] attempting to access account [{}]", user.getUsername(), accountNumber);

        Account account = accountService.getAuthorizedAccount(accountNumber, user, isAdmin);
        AccountResponse response = accountService.mapToResponse(account);

        logger.info("Access granted for account [{}] to user [{}]", accountNumber, user.getUsername());
        return ResponseEntity.ok(response);
    }

    //done
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PutMapping("/deactivate/{accountNumber}")
    public ResponseEntity<?> deactivateAccount(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccountNotFoundException, AccessDeniedException {
        User user = (User) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);

        logger.info("User [{}] attempting to deactivate account [{}]", user.getUsername(), accountNumber);
        Account account = accountService.getAccountByNumber(accountNumber);
        accountService.verifyOwnershipOrAdmin(account, user, isAdmin);

        boolean changed = accountService.deactivateAccount(accountNumber);
        return changed
                ? ResponseEntity.ok(Map.of("message", "Account deactivated successfully."))
                : ResponseEntity.ok(Map.of("message", "Account was already deactivated."));
    }

    //done
    @PutMapping("/activate/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> activateAccount(
            @PathVariable String accountNumber,
            Authentication authentication
    ) throws AccountNotFoundException, AccessDeniedException {

        User user = (User) authentication.getPrincipal();
        boolean isAdmin = isAdmin(authentication);

        logger.info("User [{}] attempting to activate account [{}]", user.getUsername(), accountNumber);

        Account account = accountService.getAccountByNumber(accountNumber);

        accountService.verifyOwnershipOrAdmin(account, user, isAdmin);

        boolean changed = accountService.activateAccount(accountNumber);
        return changed
                ? ResponseEntity.ok(Map.of("message", "Account activated successfully."))
                : ResponseEntity.ok(Map.of("message", "Account was already active."));
    }

}

