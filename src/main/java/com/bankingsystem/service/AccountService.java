package com.bankingsystem.service;

import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.AccountRequest;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.enums.AccountType;
import com.bankingsystem.entity.enums.RequestStatus;
import com.bankingsystem.exception.UserNotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.AccountRequestRepository;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.util.AccountNumberGenerator;
import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.exception.AccountRequestNotFoundException;
import com.bankingsystem.exception.AdminSelfAccountCreationException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountRequestRepository accountRequestRepository;

    public AccountResponse createAccount(String userId, String accountType, boolean isAdmin) {
        logger.debug("Creating account (type: {}) for user: {} (isAdmin={})", accountType, userId, isAdmin);

        AccountType type;
        try {
            type = AccountType.valueOf(accountType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid account type provided: {}", accountType);
            throw new IllegalArgumentException("Invalid account type: " + accountType);
        }

        if (isAdmin) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

            if (user.getRole().name().equalsIgnoreCase("ADMIN")) {
                logger.warn("Admin [{}] attempted to create account for themselves", user.getUsername());
                throw new AdminSelfAccountCreationException("Admins cannot create accounts for themselves.");
            }

            // Create an account for customer
            Account account = Account.builder()
                    .userId(userId)
                    .accountNumber(AccountNumberGenerator.generate())
                    .balance(BigDecimal.ZERO)
                    .accountType(type)
                    .active(true)
                    .openedAt(LocalDateTime.now())
                    .build();
            logger.info("Account created for user [{}]", user.getUsername());
            return mapToResponse(accountRepository.save(account));
        } else {
            // Save as a request for approval
            AccountRequest request = AccountRequest.builder()
                    .userId(userId)
                    .accountType(type)
                    .requestedAt(LocalDateTime.now())
                    .status(RequestStatus.PENDING)
                    .build();
            accountRequestRepository.save(request);
            logger.info("Account creation request submitted for userId [{}]", userId);
            return AccountResponse.builder()
                    .id(request.getId())
                    .accountNumber("REQUESTED")
                    .accountType(type)
                    .balance(BigDecimal.ZERO)
                    .isActive(false)
                    .maskedAccountNumber("REQUESTED")
                    .openAt(null)
                    .build();
        }
    }

    public List<AccountRequest> getPendingAccountRequests() {
        logger.debug("Fetching pending account requests");
        return accountRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    public String handleAccountRequest(String requestId, boolean approve) {
        logger.debug("Processing account request [{}], approve: {}", requestId, approve);

        AccountRequest request = accountRequestRepository.findById(requestId)
                .orElseThrow(() -> new AccountRequestNotFoundException("Account request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            logger.warn("Request [{}] already processed with status [{}]", requestId, request.getStatus());
            return "Request already processed.";
        }

        if (approve) {
            // Create an account
            Account account = Account.builder()
                    .userId(request.getUserId())
                    .accountNumber(AccountNumberGenerator.generate())
                    .balance(BigDecimal.ZERO)
                    .accountType(request.getAccountType())
                    .active(true)
                    .openedAt(LocalDateTime.now())
                    .build();
            accountRepository.save(account);
            request.setStatus(RequestStatus.APPROVED);
        } else {
            request.setStatus(RequestStatus.REJECTED);
        }

        accountRequestRepository.save(request);
        logger.info("Account request [{}] processed as [{}]", requestId, request.getStatus());
        return "Request " + request.getStatus().name().toLowerCase() + " successfully.";
    }

    public List<Account> getAccountsByUserId(String userId) {
        // Check if user exists
        if (!userRepository.existsById(userId)) {
            logger.info("No accounts found for user ID: {}", userId);
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        // Return accounts (empty list if none)
        return accountRepository.findByUserId(userId);
    }

    public Account getAccountByNumber(String accountNumber) {
        // Add logging before and after
        logger.debug("Fetching account by number: {}", accountNumber);

        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountNumber));
    }

    public boolean deactivateAccount(String accountNumber) throws AccountNotFoundException {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> {
                    if (!account.isActive())
                        return false; // Already inactive
                    account.setActive(false);
                    accountRepository.save(account);
                    logger.info("Account [{}] deactivated", accountNumber);
                    return true;
                })
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    public boolean activateAccount(String accountNumber) throws AccountNotFoundException {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> {
                    if (isActive(account))
                        return false; // Already active
                    account.setActive(true);
                    accountRepository.save(account);
                    logger.info("Account [{}] activated", accountNumber);
                    return true;
                })
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    private static boolean isActive(Account account) {
        return account.isActive();
    }

    public AccountResponse mapToResponse(Account account) {
        return AccountResponse.fromAccount(account);
    }

    public Account getAccountByIdAndUsername(String accountNumber, String username)
            throws AccountNotFoundException, AccessDeniedException {
        // Find user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        // Find an account by id
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountNumber));

        if (!account.isActive()) {
            throw new AccountNotFoundException("Account is inactive.");
        }

        // Verify that account belongs to user
        if (!account.getUserId().equals(user.getId())) {
            throw new AccessDeniedException("Account does not belong to the user");
        }

        return account;
    }

    public void verifyOwnershipOrAdmin(Account account, User user, boolean isAdmin) throws AccessDeniedException {
        if (!isAdmin && !account.getUserId().equals(user.getId())) {
            logger.warn("Unauthorized access attempt by user [{}] to account [{}]", user.getUsername(), account.getAccountNumber());
            throw new AccessDeniedException("You do not have permission to access this account.");
        }
    }

    public Account getAuthorizedAccount(String accountNumber, User user, boolean isAdmin)
            throws AccountNotFoundException, AccessDeniedException {
        Account account = getAccountByNumber(accountNumber);
        if (!account.isActive()) {
            logger.warn("Access attempt to inactive account [{}] by user [{}]", accountNumber, user.getUsername());
            throw new AccountNotFoundException("Account is inactive.");
        }
        verifyOwnershipOrAdmin(account, user, isAdmin);
        return account;
    }

}

