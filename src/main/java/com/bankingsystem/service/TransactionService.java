package com.bankingsystem.service;

import com.bankingsystem.dto.request.TransactionRequest;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.User;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.TransactionRepository;
import com.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {


    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction deposit(TransactionRequest request) {
        Optional<Account> accountOpt = accountRepository.findById(request.getTargetAccountId());
        if (accountOpt.isEmpty()) {
            throw new RuntimeException("Target account not found.");
        }

        Account account = accountOpt.get();
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setType("DEPOSIT");
        transaction.setAmount(request.getAmount());
        transaction.setTargetAccountId(account.getId());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transaction.setDescription(request.getDescription());

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction withdraw(TransactionRequest request) {
        Optional<Account> accountOpt = accountRepository.findById(request.getSourceAccountId());
        if (accountOpt.isEmpty()) {
            throw new RuntimeException("Source account not found.");
        }

        Account account = accountOpt.get();
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance.");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setType("WITHDRAW");
        transaction.setAmount(request.getAmount());
        transaction.setSourceAccountId(account.getId());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transaction.setDescription(request.getDescription());

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction transfer(TransactionRequest request) {
        Optional<Account> fromAccountOpt = accountRepository.findById(request.getSourceAccountId());
        Optional<Account> toAccountOpt = accountRepository.findById(request.getTargetAccountId());

        if (fromAccountOpt.isEmpty() || toAccountOpt.isEmpty()) {
            throw new RuntimeException("One or both accounts not found.");
        }

        Account fromAccount = fromAccountOpt.get();
        Account toAccount = toAccountOpt.get();

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance in source account.");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = new Transaction();
        transaction.setType("TRANSFER");
        transaction.setAmount(request.getAmount());
        transaction.setSourceAccountId(fromAccount.getId());
        transaction.setTargetAccountId(toAccount.getId());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transaction.setDescription(request.getDescription());

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionsForAccount(String accountId) {
        return transactionRepository.findBySourceAccountIdOrTargetAccountId(accountId, accountId);
    }

    public boolean isAccountOwnedByUser(String accountId, String username) {
        // Find the user by username
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            return false;
        }

        // Find the account by accountId (or accountNumber)
        Account account = accountRepository.findById(accountId)
                .orElse(null);

        if (account == null) {
            return false;
        }

        // Compare userId of the account with the user's id
        return account.getUserId().equals(user.getId());
    }
}
