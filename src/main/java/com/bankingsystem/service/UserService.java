package com.bankingsystem.service;

import com.bankingsystem.dto.response.AccountResponse;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.User;
import com.bankingsystem.exception.UserNotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
    }

    public User updateUser(String userId, User updatedUser) {
        User existingUser = getUserById(userId);

        // Username uniqueness check
        if (updatedUser.getUsername() != null && !updatedUser.getUsername().isBlank()) {
            userRepository.findByUsernameAndIdNot(updatedUser.getUsername(), userId)
                    .ifPresent(user -> {
                        throw new IllegalArgumentException("Username is already taken.");
                    });
            existingUser.setUsername(updatedUser.getUsername());
        }

        // Email uniqueness check
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isBlank()) {
            userRepository.findByEmailAndIdNot(updatedUser.getEmail(), userId)
                    .ifPresent(user -> {
                        throw new IllegalArgumentException("Email is already in use.");
                    });
            existingUser.setEmail(updatedUser.getEmail());
        }

        // Allow role update only if explicitly set
        if (updatedUser.getRole() != null) {
            existingUser.setRole(updatedUser.getRole());
        }

        // Handle password update securely
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        return userRepository.save(existingUser);
    }


    public UserResponse getCurrentUserWithAccounts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();


        if (username == null || username.isBlank()) {
            throw new UserNotFoundException("No authenticated user found.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));


        List<Account> accounts = accountRepository.findByUserId(user.getId());
        List<AccountResponse> accountResponses = accounts.stream()
                .map(AccountResponse::fromAccount)
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                accountResponses
        );
    }

}
