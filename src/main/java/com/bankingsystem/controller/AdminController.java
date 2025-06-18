package com.bankingsystem.controller;


import com.bankingsystem.dto.response.TransactionResponse;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.AccountRequest;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.enums.TransactionStatus;
import com.bankingsystem.service.AccountService;
import com.bankingsystem.service.TransactionService;
import com.bankingsystem.service.UserService;
import com.bankingsystem.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {


    private final UserService userService;
    private final TransactionService transactionService;
    private final AccountService accountService;

    //done
    // ADMIN: Get current user details
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-user/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id, Authentication authentication) {
        User user = userService.getUserById(id);

        if (!AuthUtils.isOwnerOrAdmin(user, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: You can only access your own data."));
        }
        UserResponse response = userService.mapToUserResponse(user);
        return ResponseEntity.ok(response);
    }

    //done
    // ADMIN: Get all users
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = users.stream()
                .map(userService::mapToUserResponse)
                .toList();
        if (responses.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No users found."));
        }
        
        return ResponseEntity.ok(responses);
    }

    //done
    // ADMIN: Process account requests (approve or reject)
    @PutMapping("/requests-account-process/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processAccountRequest(
            @PathVariable String requestId,
            @RequestParam boolean approve) {
        String result = accountService.handleAccountRequest(requestId, approve);
        return ResponseEntity.ok(Map.of("message", result));
    }


    // Combined endpoint could be added like this if you want one endpoint:
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/process/transaction/{transactionId}")
    public ResponseEntity<TransactionResponse> processTransaction(
        @PathVariable String transactionId,
        @RequestParam("action") String action // "approve" or "reject"
    ) {
        TransactionStatus status;
        switch (action.toLowerCase()) {
            case "approve" -> status = TransactionStatus.SUCCESS;
            case "reject" -> status = TransactionStatus.REJECTED;
            default -> throw new IllegalArgumentException("Invalid action. Must be 'approve' or 'reject'.");
        }

        Transaction transaction = transactionService.processTransaction(transactionId, status);
        return ResponseEntity.ok(transactionService.mapToResponse(transaction));
    }





    //done
    // ADMIN: View all pending transactions
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending-transactions")
    public ResponseEntity<?> getPendingTransactions() {
        List<TransactionResponse> pendingTransactions = transactionService.getPendingTransactions();
        if (pendingTransactions.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No Pending transactions found."));

        }
        return ResponseEntity.ok(
                transactionService.getPendingTransactions());
    }

    //done
    // ADMIN: View all account requests
    @GetMapping("/request-accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAccountRequests() {
        List<AccountRequest> pendingAccountRequests = accountService.getPendingAccountRequests();
        if (pendingAccountRequests.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No Pending accounts requests found."));
        }
        return ResponseEntity.ok(pendingAccountRequests);
    }
}

