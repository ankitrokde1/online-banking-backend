package com.bankingsystem.service;

import com.bankingsystem.dto.request.TransactionRequest;
import com.bankingsystem.dto.response.TransactionResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.enums.TransactionStatus;
import com.bankingsystem.entity.enums.TransactionType;
import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.exception.InactiveAccountException;
import com.bankingsystem.exception.InsufficientBalanceException;
import com.bankingsystem.exception.InvalidTransferException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.TransactionRepository;
import com.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction deposit(TransactionRequest request) {
       
        validateAmount(request.getAmount(), "Deposit");
        Account account = getAccountById(request.getTargetAccountId(), "Target");
        ensureAccountIsActive(account, "Target");
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        return saveTransaction(TransactionType.DEPOSIT, null, account.getId(), request);
    }

    
    @Transactional
    public Transaction withdraw(TransactionRequest request) {
       
        validateAmount(request.getAmount(), "Withdraw");
        Account account = getAccountById(request.getSourceAccountId(), "Source");
        ensureAccountIsActive(account, "Source");
        validateSufficientBalance(account, request.getAmount(), "withdrawal");
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        return saveTransaction(TransactionType.WITHDRAW, account.getId(), null, request);
    }


    @Transactional
    public Transaction transfer(TransactionRequest request) {
        
        validateAmount(request.getAmount(), "Transfer");
        Account fromAccount = getAccountById(request.getSourceAccountId(), "Source");
        Account toAccount = getAccountById(request.getTargetAccountId(), "Target");

        ensureAccountIsActive(fromAccount, "Source");
        ensureAccountIsActive(toAccount, "Target");


        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new InvalidTransferException("Cannot transfer to the same account.");
        }
        
        validateSufficientBalance(fromAccount, request.getAmount(), "transfer");

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return saveTransaction(TransactionType.TRANSFER, fromAccount.getId(), toAccount.getId(), request);
    }

    public List<Transaction> getTransactionsForAccountWithAuthorization(String accountId, Authentication auth) {
        if (!accountExists(accountId)) {
            throw new AccountNotFoundException("Account not found with id: " + accountId);
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !isAccountOwnedByUser(accountId, auth.getName())) {
            throw new AccessDeniedException("Access denied. You can only view your own account transactions.");
        }

        return getTransactionsForAccount(accountId);
    }

    private List<Transaction> getTransactionsForAccount(String accountId) {
        return transactionRepository.findBySourceAccountIdOrTargetAccountId(accountId, accountId);
    }

    public boolean isAccountOwnedByUser(String accountId, String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> accountRepository.findById(accountId)
                        .map(account -> account.getUserId().equals(user.getId())))
                .orElse(false);
    }

    public boolean accountExists(String accountId) {
        return accountRepository.existsById(accountId);
    }



    // Mapper method
    public TransactionResponse mapToResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getSourceAccountId(), // fromAccountId
                tx.getTargetAccountId(), // toAccountId
                tx.getAmount(),
                tx.getType(),
                tx.getTimestamp());
    }
    
    // ✅ [Helper] Validate transaction amount
    private void validateAmount(BigDecimal amount, String operation) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(operation + " amount must be greater than zero.");
        }
    }

    // ✅ [Helper] Get account or throw custom exception
    private Account getAccountById(String accountId, String label) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(label + " account not found."));
    }

    // ✅ [Helper] Save transaction
    private Transaction saveTransaction(TransactionType type, String sourceId, String targetId,
                                        TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setAmount(request.getAmount());
        transaction.setSourceAccountId(sourceId);
        transaction.setTargetAccountId(targetId);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.SUCCESS);

        String description = request.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = switch (type) {
                case DEPOSIT -> "Amount deposited to account";
                case WITHDRAW -> "Amount withdrawn from account";
                case TRANSFER -> "Amount transferred between accounts";
            };
        }
        transaction.setDescription(description);

        return transactionRepository.save(transaction);
    }


    private void validateSufficientBalance(Account account, BigDecimal amount, String operation) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for " + operation + ".");
        }
    }

    private void ensureAccountIsActive(Account account, String label) {
        if (!account.isActive()) {
            throw new InactiveAccountException(label + " account is inactive.");
        }
    }
}
