package com.bankingsystem.controller;

import com.bankingsystem.dto.request.TransactionRequest;
import com.bankingsystem.dto.response.TransactionResponse;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.deposit(request);
        return ResponseEntity.ok(mapToResponse(transaction));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.withdraw(request);
        return ResponseEntity.ok(mapToResponse(transaction));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/transfer")
    @Transactional
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.transfer(request);
        return ResponseEntity.ok(mapToResponse(transaction));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getAllForAccount(@PathVariable String accountId) {
//        String username = authentication.getName();
//        if (!transactionService.isAccountOwnedByUser(accountId, username)) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
//        }
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
