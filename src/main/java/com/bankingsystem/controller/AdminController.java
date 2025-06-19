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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final TransactionService transactionService;
    private final AccountService accountService;

    //done
    // ADMIN: Get current user details
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-user/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id, Authentication authentication) {

        logger.info("Admin [{}] is requesting user details for userId [{}]", authentication.getName(), id);

        User user = userService.getUserById(id);

        if (!AuthUtils.isOwnerOrAdmin(user, authentication)) {
            logger.warn("Unauthorized access attempt by [{}] for userId [{}]", authentication.getName(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: You can only access your own data."));
        }
        UserResponse response = userService.mapToUserResponse(user);
        logger.info("Successfully retrieved user details for userId [{}]", id);
        return ResponseEntity.ok(response);
    }

    //done
    // ADMIN: Get all users
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-users")
    public ResponseEntity<?> getAllUsers() {

        logger.info("Admin is retrieving all users");

        List<User> users = userService.getAllUsers();

        List<UserResponse> responses = users.stream()
                .map(userService::mapToUserResponse)
                .toList();
        if (responses.isEmpty()) {
            logger.warn("No users found in the system");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No users found."));
        }

        logger.info("Admin fetched [{}] users", responses.size());
        return ResponseEntity.ok(responses);
    }

    //done
    // ADMIN: Process account requests (approve or reject)
    @PutMapping("/requests-account-process/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processAccountRequest(
            @PathVariable String requestId,
            @RequestParam boolean approve) {

        logger.info("Admin is processing account request [{}] with action [{}]",
                requestId, approve ? "APPROVE" : "REJECT");

        String result = accountService.handleAccountRequest(requestId, approve);
        logger.info("Account request [{}] processed: {}", requestId, result);
        return ResponseEntity.ok(Map.of("message", result));
    }


    //done
    // Combined endpoint could be added like this if you want one endpoint:
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/process/transaction/{transactionId}")
    public ResponseEntity<TransactionResponse> processTransaction(
        @PathVariable String transactionId,
        @RequestParam("action") String action // "approve" or "reject"
    ) {

        logger.info("Admin is processing transaction [{}] with action [{}]", transactionId, action);

        TransactionStatus status;
        switch (action.toLowerCase()) {
            case "approve" -> status = TransactionStatus.SUCCESS;
            case "reject" -> status = TransactionStatus.REJECTED;
            default -> {
                logger.error("Invalid action provided: {}", action);
                throw new IllegalArgumentException("Invalid action. Must be 'approve' or 'reject'.");
            }
        }

        Transaction transaction = transactionService.processTransaction(transactionId, status);

        logger.info("Transaction [{}] processed with status [{}]", transactionId, status);
        return ResponseEntity.ok(transactionService.mapToResponse(transaction));
    }

    //done
    // ADMIN: View all pending transactions
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending-transactions")
    public ResponseEntity<?> getPendingTransactions() {

        logger.info("Admin requested list of pending transactions");

        List<TransactionResponse> pendingTransactions = transactionService.getPendingTransactions();
        if (pendingTransactions.isEmpty()) {
            logger.warn("No pending transactions found");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No Pending transactions found."));

        }

        logger.info("Found [{}] pending transactions", pendingTransactions.size());

        return ResponseEntity.ok(
                transactionService.getPendingTransactions());
    }

    //done
    // ADMIN: View all account requests
    @GetMapping("/request-accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAccountRequests() {

        logger.info("Admin requested pending account opening requests");

        List<AccountRequest> pendingAccountRequests = accountService.getPendingAccountRequests();

        if (pendingAccountRequests.isEmpty()) {
            logger.warn("No pending account requests found");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No Pending accounts requests found."));
        }

        logger.info("Found [{}] pending account requests", pendingAccountRequests.size());
        return ResponseEntity.ok(pendingAccountRequests);
    }
}

