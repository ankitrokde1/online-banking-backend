package com.bankingsystem.controller;

import com.bankingsystem.dto.request.TransactionRequest;
import com.bankingsystem.dto.response.TransactionResponse;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
@Validated
public class TransactionController {


    private final TransactionService transactionService;

    //done*
    @PostMapping("/request-deposit")
    public ResponseEntity<TransactionResponse> requestDeposit(
            @Valid @RequestBody TransactionRequest request, Authentication authentication) {
        Transaction transaction = transactionService.requestDeposit(request, authentication);
        return ResponseEntity.ok(transactionService.mapToResponse(transaction));
    }

    //done
    @PostMapping("/request-withdraw")
    public ResponseEntity<TransactionResponse> requestWithdraw(
            @Valid @RequestBody TransactionRequest request, Authentication authentication) {
        Transaction transaction = transactionService.requestWithdraw(request, authentication);
        return ResponseEntity.ok(transactionService.mapToResponse(transaction));
    }

    //done
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransactionRequest request,
            Authentication authentication) {
        Transaction transaction = transactionService.transfer(request, authentication);
        return ResponseEntity.ok(transactionService.mapToResponse(transaction));
    }

    //done
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAllForAccount(@PathVariable String accountId, Authentication authentication) {

        List<Transaction> transactions = transactionService.getTransactionsForAccountWithAuthorization(accountId, authentication);
        List<TransactionResponse> responses = transactions.stream()
                .map(transactionService::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

}
