package com.bankingsystem.service;

import com.bankingsystem.dto.request.TransactionRequest;
import com.bankingsystem.dto.response.TransactionResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.enums.TransactionStatus;
import com.bankingsystem.entity.enums.TransactionType;
import com.bankingsystem.exception.*;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.TransactionRepository;
import com.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction requestDeposit(TransactionRequest request, Authentication auth) {
        logger.debug("Processing deposit request by [{}] to [{}] amount [{}]",
                auth.getName(), request.getTargetAccountId(), request.getAmount());

        validateAmount(request.getAmount(), "Deposit");
        Account account = getAccountById(request.getTargetAccountId(), "Target");
        ensureAccountIsActive(account, "Target");

        User user = getAuthenticatedUser(auth);
        boolean isAdmin = isAdmin(auth);
        boolean isOwnAccount = account.getUserId().equals(user.getId());

        if (!isAdmin) {
            return saveTransaction(TransactionType.DEPOSIT, null, account.getId(), request, TransactionStatus.PENDING);
        }

        if (isOwnAccount) {
            throw new AccessDeniedException("Admins cannot deposit into their own account.");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        logger.info("Deposit transaction created. Status: {}, Account ID: {}", TransactionStatus.SUCCESS, account.getId());
        return saveTransaction(TransactionType.DEPOSIT, null, account.getId(), request, TransactionStatus.SUCCESS);
    }

    @Transactional
    public Transaction requestWithdraw(TransactionRequest request, Authentication auth) {

        logger.debug("Processing withdrawal request by [{}] from [{}] amount [{}]",
                auth.getName(), request.getSourceAccountId(), request.getAmount());

        validateAmount(request.getAmount(), "Withdraw");
        Account account = getAccountById(request.getSourceAccountId(), "Source");
        ensureAccountIsActive(account, "Source");
        validateSufficientBalance(account, request.getAmount(), "withdrawal");

        User user = getAuthenticatedUser(auth);
        boolean isAdmin = isAdmin(auth);
        boolean isOwnAccount = account.getUserId().equals(user.getId());

        if (!isAdmin) {
            return saveTransaction(TransactionType.WITHDRAW, account.getId(), null, request, TransactionStatus.PENDING);
        }

        if (isOwnAccount) {
            throw new AccessDeniedException("Admins cannot withdraw from their own account.");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        logger.info("Withdrawal transaction created. Status: {}, Account ID: {}", TransactionStatus.SUCCESS, account.getId());
        return saveTransaction(TransactionType.WITHDRAW, account.getId(), null, request, TransactionStatus.SUCCESS);
    }

    @Transactional
    public Transaction transfer(TransactionRequest request, Authentication auth) {

        logger.debug("Processing transfer by [{}] from [{}] to [{}] amount [{}]",
                auth.getName(), request.getSourceAccountId(), request.getTargetAccountId(), request.getAmount());

        validateAmount(request.getAmount(), "Transfer");
        Account fromAccount = getAccountById(request.getSourceAccountId(), "Source");
        Account toAccount = getAccountById(request.getTargetAccountId(), "Target");

        ensureAccountIsActive(fromAccount, "Source");
        ensureAccountIsActive(toAccount, "Target");

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new InvalidTransferException("Cannot transfer to the same account.");
        }

        validateSufficientBalance(fromAccount, request.getAmount(), "transfer");

        User user = getAuthenticatedUser(auth);
        boolean isAdmin = isAdmin(auth);
        boolean adminInvolved = fromAccount.getUserId().equals(user.getId())
                || toAccount.getUserId().equals(user.getId());

        if (isAdmin && adminInvolved) {
            logger.error("Admin [{}] attempted illegal transfer involving own account", auth.getName());
            throw new AccessDeniedException("Admins cannot transfer to/from their own accounts.");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        logger.info("Transfer transaction successful. Amount: {}, From: {}, To: {}",
                request.getAmount(), fromAccount.getId(), toAccount.getId());

        return saveTransaction(TransactionType.TRANSFER, fromAccount.getId(), toAccount.getId(), request,
                TransactionStatus.SUCCESS);
    }
    
    @Transactional
    public Transaction processTransaction(String id, TransactionStatus action) {

        logger.debug("Processing transaction [{}] with action [{}]", id, action);

        Transaction tx = getTransactionById(id);

        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only pending transactions can be processed.");
        }

        if (action == TransactionStatus.SUCCESS) {
            if (tx.getType() == TransactionType.DEPOSIT) {
                Account account = getAccountById(tx.getTargetAccountId(), "Target");
                account.setBalance(account.getBalance().add(tx.getAmount()));
                accountRepository.save(account);
                
            } else if (tx.getType() == TransactionType.WITHDRAW) {
                Account account = getAccountById(tx.getSourceAccountId(), "Source");
                validateSufficientBalance(account, tx.getAmount(), "withdrawal approval");
                account.setBalance(account.getBalance().subtract(tx.getAmount()));
                accountRepository.save(account);
            }
        }

        tx.setStatus(action);
        logger.info("Transaction [{}] of type [{}] updated to status [{}]", id, tx.getType(), action);
        return transactionRepository.save(tx);
    }

    public List<TransactionResponse> getPendingTransactions() {
        logger.info("Fetching all pending transactions");

        return transactionRepository.findByStatus(TransactionStatus.PENDING).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<Transaction> getTransactionsForAccountWithAuthorization(String accountId, Authentication auth) {

        logger.debug("Authorizing transaction history access for [{}] on account [{}]", auth.getName(), accountId);

        if (!accountExists(accountId)) {
            throw new AccountNotFoundException("Account not found with id: " + accountId);
        }

        boolean isAdmin = isAdmin(auth);
        if (!isAdmin && !isAccountOwnedByUser(accountId, auth.getName())) {
            throw new AccessDeniedException("Access denied. You can only view your own account transactions.");
        }

        logger.info("Transaction access granted for account [{}]", accountId);
        return transactionRepository.findBySourceAccountIdOrTargetAccountId(accountId, accountId);
    }

    // -------------------- HELPER METHODS --------------------

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void validateAmount(BigDecimal amount, String operation) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid {} amount: {}", operation, amount);
            throw new IllegalArgumentException(operation + " amount must be greater than zero.");
        }
    }

    private void ensureAccountIsActive(Account account, String label) {
        if (!account.isActive()) {
            throw new InactiveAccountException(label + " account is inactive.");
        }
    }

    private void validateSufficientBalance(Account account, BigDecimal amount, String operation) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for " + operation + ".");
        }
    }

    private Account getAccountById(String accountId, String label) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(label + " account not found."));
    }

    private Transaction getTransactionById(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found."));
    }

    private boolean isAccountOwnedByUser(String accountId, String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> accountRepository.findById(accountId)
                        .map(account -> account.getUserId().equals(user.getId())))
                .orElse(false);
    }

    private boolean accountExists(String accountId) {
        return accountRepository.existsById(accountId);
    }

    public TransactionResponse mapToResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getSourceAccountId(),
                tx.getTargetAccountId(),
                tx.getAmount(),
                tx.getType(),
                tx.getStatus(),
                tx.getTimestamp()
        );
    }

    private Transaction saveTransaction(TransactionType type, String sourceId, String targetId,
                                        TransactionRequest request, TransactionStatus status) {
        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setAmount(request.getAmount());
        tx.setSourceAccountId(sourceId);
        tx.setTargetAccountId(targetId);
        tx.setStatus(status);
        tx.setTimestamp(LocalDateTime.now());

        String description = request.getDescription();
        if (description == null || description.trim().isEmpty()) {
            if (status == TransactionStatus.PENDING) {
                description = switch (type) {
                    case DEPOSIT -> "Requested deposit to account";
                    case WITHDRAW -> "Requested withdrawal from account";
                    case TRANSFER -> "Requested transfer between accounts"; // if you allow pending transfers
                };
            } else {
                description = switch (type) {
                    case DEPOSIT -> "Deposit to account";
                    case WITHDRAW -> "Withdrawal from account";
                    case TRANSFER -> "Transfer between accounts";
                };
            }
        }
        
        tx.setDescription(description);

        return transactionRepository.save(tx);
    }
}
