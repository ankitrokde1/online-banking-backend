package com.bankingsystem.controller;

import com.bankingsystem.dto.request.TransactionRequest;
import com.bankingsystem.dto.response.TransactionResponse;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionController {


    private final TransactionService transactionService;

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.deposit(request);
        return ResponseEntity.ok(mapToResponse(transaction));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.withdraw(request);
        return ResponseEntity.ok(mapToResponse(transaction));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping("/transfer")
    @Transactional
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.transfer(request);
        return ResponseEntity.ok(mapToResponse(transaction));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAllForAccount(@PathVariable String accountId, Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 1. Check if account exists
        if (!transactionService.accountExists(accountId)) {
            throw new AccountNotFoundException("Account not found with id: " + accountId);
        }

        // 2. If not admin, check ownership
        if (!isAdmin && !transactionService.isAccountOwnedByUser(accountId, username)) {
            throw new AccessDeniedException("Access denied. You can only view your own account transactions.");
        }

        List<Transaction> transactions = transactionService.getTransactionsForAccount(accountId);
        List<TransactionResponse> responses = transactions.stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }




    // Mapper method
    private TransactionResponse mapToResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getSourceAccountId(), // fromAccountId
                tx.getTargetAccountId(), // toAccountId
                tx.getAmount(),
                tx.getType(),
                tx.getTimestamp()
        );
    }

}
