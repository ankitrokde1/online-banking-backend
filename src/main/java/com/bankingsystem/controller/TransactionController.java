package com.bankingsystem.controller;

import com.bankingsystem.dto.request.TransactionRequest;
import com.bankingsystem.dto.response.TransactionResponse;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    //done*
    @PostMapping("/request-deposit")
    public ResponseEntity<TransactionResponse> requestDeposit(
            @Valid @RequestBody TransactionRequest request, Authentication authentication) {
        logger.info("Received deposit request by user [{}] for account [{}] with amount [{}]",
                authentication.getName(), request.getTargetAccountId(), request.getAmount());

        Transaction transaction = transactionService.requestDeposit(request, authentication);

        logger.info("Deposit request processed. Status: {}, Txn ID: {}",
                transaction.getStatus(), transaction.getId());

        return ResponseEntity.ok(transactionService.mapToResponse(transaction));
    }

    //done
    @PostMapping("/request-withdraw")
    public ResponseEntity<TransactionResponse> requestWithdraw(
            @Valid @RequestBody TransactionRequest request, Authentication authentication) {
        logger.info("Received withdraw request by user [{}] from account [{}] with amount [{}]",
                authentication.getName(), request.getSourceAccountId(), request.getAmount());

        Transaction transaction = transactionService.requestWithdraw(request, authentication);

        logger.info("Withdraw request processed. Status: {}, Txn ID: {}",
                transaction.getStatus(), transaction.getId());

        return ResponseEntity.ok(transactionService.mapToResponse(transaction));
    }

    //done
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransactionRequest request,
                                                        Authentication authentication) {
        logger.info("Transfer request by user [{}] from [{}] to [{}] amount [{}]",
                authentication.getName(), request.getSourceAccountId(), request.getTargetAccountId(), request.getAmount());

        Transaction transaction = transactionService.transfer(request, authentication);

        logger.info("Transfer completed. Txn ID: {}, Amount: {}, Status: {}",
                transaction.getId(), transaction.getAmount(), transaction.getStatus());

        return ResponseEntity.ok(transactionService.mapToResponse(transaction));
    }

    //done
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAllForAccount(@PathVariable String accountId, Authentication authentication) {

        logger.info("Fetching transactions for account [{}] requested by [{}]", accountId, authentication.getName());

        List<Transaction> transactions = transactionService.getTransactionsForAccountWithAuthorization(accountId, authentication);

        logger.info("Found [{}] transactions for account [{}]", transactions.size(), accountId);

        List<TransactionResponse> responses = transactions.stream()
                .map(transactionService::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

}
